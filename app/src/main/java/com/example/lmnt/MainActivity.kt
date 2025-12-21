package com.example.lmnt

import android.content.ComponentName
import android.content.ContentUris
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.viewpager2.widget.ViewPager2
import coil.load
import com.example.lmnt.model.Album
import com.example.lmnt.model.Artist
import com.example.lmnt.Song
import com.example.lmnt.service.PlaybackService
import com.example.lmnt.ui.AlbumsFragment
import com.example.lmnt.ui.ArtistsFragment
import com.example.lmnt.ui.theme.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.common.util.concurrent.ListenableFuture

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var myAdapter: ViewPagerAdapter
    private var controllerFuture: ListenableFuture<MediaController>? = null
    var mediaController: MediaController? = null

    fun loadAlbenForArtist(artistName: String): List<com.example.lmnt.model.Album> {
        val albumList = mutableListOf<com.example.lmnt.model.Album>()
        val projection = arrayOf(
            MediaStore.Audio.Albums._ID,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.NUMBER_OF_SONGS
        )

        // Wir suchen Alben, die genau diesem Künstlernamen entsprechen
        val selection = "${MediaStore.Audio.Albums.ARTIST} = ?"
        val selectionArgs = arrayOf(artistName)

        contentResolver.query(
            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Audio.Albums.ALBUM} ASC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)
            val countCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                // Erzeuge die URI für das Album-Cover
                val artworkUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    id
                ).toString()

                albumList.add(com.example.lmnt.model.Album(
                    id = id,
                    title = cursor.getString(albumCol) ?: "Unbekanntes Album",
                    artist = cursor.getString(artistCol) ?: "Unbekannt",
                    artworkUri = artworkUri,
                    songCount = cursor.getInt(countCol)
                ))
            }
        }
        return albumList
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // UI Initialisierung
        viewPager = findViewById(R.id.viewPager)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        val normalLayout = findViewById<LinearLayout>(R.id.normalToolbarLayout)
        val customSearchView = findViewById<SearchView>(R.id.customSearchView)
        val btnSearch = findViewById<ImageButton>(R.id.btnSearch)

        setupViewPager()

        // --- MANUELLE SUCH-LOGIK ---
        btnSearch.setOnClickListener {
            normalLayout.visibility = View.GONE
            customSearchView.visibility = View.VISIBLE
            customSearchView.isIconified = false
            customSearchView.requestFocus()
        }

        customSearchView.setOnCloseListener {
            customSearchView.visibility = View.GONE
            normalLayout.visibility = View.VISIBLE
            false
        }

        customSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                val queryText = newText ?: ""
                val currentPos = viewPager.currentItem
                val currentFragment = myAdapter.getFragment(currentPos)

                when(currentPos) {
                    1 -> (currentFragment as? SongsFragment)?.filter(queryText)
                    2 -> (currentFragment as? AlbumsFragment)?.filter(queryText)
                    3 -> (currentFragment as? ArtistsFragment)?.filter(queryText)
                }
                return true
            }
        })
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val fragmentContainer = findViewById<View>(R.id.fragment_container)
                val customSearchView = findViewById<SearchView>(R.id.customSearchView)
                val normalLayout = findViewById<LinearLayout>(R.id.normalToolbarLayout)

                if (customSearchView.visibility == View.VISIBLE) {
                    // 1. Suche schließen
                    customSearchView.visibility = View.GONE
                    normalLayout.visibility = View.VISIBLE
                } else if (supportFragmentManager.backStackEntryCount > 0) {
                    // 2. Fragment zurückgehen
                    supportFragmentManager.popBackStack()
                    // Container verstecken, wenn das letzte Fragment geschlossen wurde
                    if (supportFragmentManager.backStackEntryCount <= 1) {
                        fragmentContainer.visibility = View.GONE
                    }
                } else {
                    // 3. App ganz normal verlassen
                    isEnabled = false // Callback deaktivieren
                    onBackPressedDispatcher.onBackPressed() // Standard-Zurück ausführen
                    isEnabled = true // Wieder aktivieren für den nächsten Start
                }
            }
        })
    }

    private fun setupViewPager() {
        val fragments = listOf(
            HomeFragment(),
            SongsFragment(),
            AlbumsFragment(),
            ArtistsFragment(),
            PlaylistsFragment()
        )

        myAdapter = ViewPagerAdapter(this, fragments)
        viewPager.adapter = myAdapter

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> viewPager.currentItem = 0
                R.id.nav_songs -> viewPager.currentItem = 1
                R.id.nav_albums -> viewPager.currentItem = 2
                R.id.nav_artists -> viewPager.currentItem = 3
                R.id.nav_playlists -> viewPager.currentItem = 4
            }
            true
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (position < bottomNavigation.menu.size()) {
                    bottomNavigation.menu.getItem(position).isChecked = true
                }
            }
        })
    }



    // --- MEDIASTORE LOADER ---

    fun loadArtists(): List<Artist> {
        val artistList = mutableListOf<Artist>()
        val projection = arrayOf(
            MediaStore.Audio.Artists._ID,
            MediaStore.Audio.Artists.ARTIST,
            MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
            MediaStore.Audio.Artists.NUMBER_OF_TRACKS
        )
        contentResolver.query(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, projection, null, null, "${MediaStore.Audio.Artists.ARTIST} ASC")?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST)
            val albumsCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS)
            val tracksCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_TRACKS)
            while (cursor.moveToNext()) {
                artistList.add(Artist(cursor.getLong(idCol), cursor.getString(artistCol) ?: "Unbekannt", cursor.getInt(albumsCol), cursor.getInt(tracksCol)))
            }
        }
        return artistList
    }

    fun loadSongsForAlbum(albumId: Long): List<Song> {
        val songList = mutableListOf<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.TRACK
        )
        val selection = "${MediaStore.Audio.Media.ALBUM_ID} = ?"
        val selectionArgs = arrayOf(albumId.toString())

        contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, "${MediaStore.Audio.Media.TRACK} ASC")?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)

            while (cursor.moveToNext()) {
                val trackRaw = cursor.getInt(trackCol)
                val albumArtUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId).toString()

                songList.add(Song(
                    id = cursor.getLong(idCol),
                    title = cursor.getString(titleCol) ?: "Unbekannt",
                    artist = cursor.getString(artistCol) ?: "Unbekannt",
                    uri = cursor.getString(dataCol),
                    artworkUri = albumArtUri,
                    trackNumber = if (trackRaw >= 1000) trackRaw % 1000 else trackRaw,
                    discNumber = if (trackRaw >= 1000) trackRaw / 1000 else 1
                ))
            }
        }
        return songList
    }
    fun loadSongsForArtistName(artistName: String): List<Song> {
        val songList = mutableListOf<Song>()
        val selection = "${MediaStore.Audio.Media.ARTIST} = ?"
        val selectionArgs = arrayOf(artistName)
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.TRACK
        )

        contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)

            while (cursor.moveToNext()) {
                val albumId = cursor.getLong(albumIdCol)
                val artworkUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                ).toString()

                songList.add(Song(
                    id = cursor.getLong(idCol),
                    title = cursor.getString(titleCol) ?: "Unbekannt",
                    artist = cursor.getString(artistCol) ?: "Unbekannt",
                    uri = cursor.getString(dataCol),
                    artworkUri = artworkUri,
                    trackNumber = cursor.getInt(trackCol) % 1000,
                    discNumber = if (cursor.getInt(trackCol) >= 1000) cursor.getInt(trackCol) / 1000 else 1
                ))
            }
        }
        return songList
    }

    // --- PLAYBACK ---
    fun playPlaylist(songs: List<Song>, startIndex: Int) {
        val mediaItems = songs.map { song ->
            MediaItem.Builder()
                .setMediaId(song.id.toString())
                .setUri(Uri.parse(song.uri))
                .setMediaMetadata(MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setArtworkUri(Uri.parse(song.artworkUri))
                    .build())
                .build()
        }
        mediaController?.let {
            it.setMediaItems(mediaItems, startIndex, 0L)
            it.prepare()
            it.play()
        }
    }

    override fun onStart() {
        super.onStart()
        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                setupMiniPlayer()
            } catch (e: Exception) { e.printStackTrace() }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onStop() {
        super.onStop()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
    }

    private fun setupMiniPlayer() {
        val miniPlayerLayout = findViewById<LinearLayout>(R.id.miniPlayer)
        val titleTv = findViewById<TextView>(R.id.miniPlayerTitle)
        val artistTv = findViewById<TextView>(R.id.miniPlayerArtist)
        val albumArtIv = findViewById<ImageView>(R.id.miniPlayerAlbumArt)
        val playPauseBtn = findViewById<ImageButton>(R.id.btnMiniPlayerPlay)

        // Zu Beginn ist der Mini Player unsichtbar
        miniPlayerLayout.visibility = View.GONE

        mediaController?.addListener(object : Player.Listener {
            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                // Sobald Metadaten kommen, Player anzeigen
                if (mediaMetadata.title != null) {
                    miniPlayerLayout.visibility = View.VISIBLE
                }

                titleTv.text = mediaMetadata.title ?: "Keine Wiedergabe"
                artistTv.text = mediaMetadata.artist ?: ""
                albumArtIv.load(mediaMetadata.artworkUri) {
                    placeholder(R.drawable.ic_music_note)
                    error(R.drawable.ic_music_note)
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playPauseBtn.setImageResource(if (isPlaying) R.drawable.player_pause else R.drawable.player_play)
                // Sicherheitshalber: Wenn er spielt, muss er sichtbar sein
                if (isPlaying) miniPlayerLayout.visibility = View.VISIBLE
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                // Verstecken, wenn die Playlist vorbei ist oder ein Fehler auftritt
                if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) {
                    miniPlayerLayout.visibility = View.GONE
                }
            }
        })

        playPauseBtn.setOnClickListener {
            mediaController?.let { if (it.isPlaying) it.pause() else it.play() }
        }

        // --- SWIPE & CLICK LOGIK ---
        miniPlayerLayout.setOnTouchListener(object : View.OnTouchListener {
            private var startY = 0f
            private val SWIPE_THRESHOLD = 150 // Mindestdistanz für einen Swipe

            override fun onTouch(v: View, event: android.view.MotionEvent): Boolean {
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        startY = event.y
                        return true // Wir fangen das Event ab
                    }
                    android.view.MotionEvent.ACTION_UP -> {
                        val endY = event.y
                        val deltaY = startY - endY

                        if (deltaY > SWIPE_THRESHOLD) {
                            // Nach oben gewischt -> Fullscreen
                            openFullscreen()
                        } else if (Math.abs(deltaY) < 20) {
                            // Nur kurz getippt (kein Swipe) -> Fullscreen
                            openFullscreen()
                        }
                        return true
                    }
                }
                return false
            }
        })
    }

    // Kleine Hilfsfunktion zum Öffnen
    private fun openFullscreen() {
        PlayerFullscreenFragment().show(supportFragmentManager, "player")
    }
}