package com.example.lmnt

import android.content.ComponentName
import android.content.ContentUris
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.viewpager2.widget.ViewPager2
import com.example.lmnt.service.PlaybackService // Achte auf dein Package-Pfad!
import com.example.lmnt.ui.theme.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import coil.load

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNavigation: BottomNavigationView

    private var controllerFuture: ListenableFuture<MediaController>? = null
    var mediaController: MediaController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewPager = findViewById(R.id.viewPager)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        val fragments = listOf(
            HomeFragment(),
            SongsFragment(),
            AlbumsFragment(),
            ArtistsFragment(),
            PlaylistsFragment()
        )

        val adapter = ViewPagerAdapter(this, fragments)
        viewPager.adapter = adapter

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


    fun loadSongsForAlbum(albumId: Long): List<Song> {
        val songList = mutableListOf<Song>()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val selection = "${MediaStore.Audio.Media.ALBUM_ID} = ?"
        val selectionArgs = arrayOf(albumId.toString())

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.TRACK // Wichtig für Disc/Tracknummer
        )

        contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)

            while (cursor.moveToNext()) {
                val trackRaw = cursor.getInt(trackCol)
                // Umrechnung wie zuvor besprochen
                val disc = if (trackRaw >= 1000) trackRaw / 1000 else 1
                val track = if (trackRaw >= 1000) trackRaw % 1000 else trackRaw

                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"), albumId
                ).toString()

                songList.add(Song(
                    id = cursor.getLong(idCol),
                    title = cursor.getString(titleCol),
                    artist = cursor.getString(artistCol),
                    uri = cursor.getString(dataCol),
                    artworkUri = albumArtUri,
                    trackNumber = track,
                    discNumber = disc
                ))
            }
        }
        return songList
    }

    fun playPlaylist(songs: List<Song>, startIndex: Int) {
        val mediaItems = songs.map { song ->
            MediaItem.Builder()
                .setMediaId(song.id.toString())
                .setUri(Uri.parse(song.uri))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setArtworkUri(Uri.parse(song.artworkUri))
                        .build()
                )
                .build()
        }

        mediaController?.let { controller ->
            controller.setMediaItems(mediaItems, startIndex, 0L)
            controller.prepare()
            controller.play()
        }
    }


    fun loadArtists(): List<com.example.lmnt.model.Artist> {
        val artistList = mutableListOf<com.example.lmnt.model.Artist>()
        val uri = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI

        // Wir fügen MediaStore.Audio.Artists._ID zur Projektion hinzu
        val projection = arrayOf(
            MediaStore.Audio.Artists._ID,           // WICHTIG: Die ID für deine Klasse
            MediaStore.Audio.Artists.ARTIST,
            MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
            MediaStore.Audio.Artists.NUMBER_OF_TRACKS
        )

        contentResolver.query(uri, projection, null, null, "${MediaStore.Audio.Artists.ARTIST} ASC")?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID) // Neu
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST)
            val albumsCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS)
            val tracksCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_TRACKS)

            while (cursor.moveToNext()) {
                artistList.add(com.example.lmnt.model.Artist(
                    id = cursor.getLong(idCol),      // Jetzt wird die ID korrekt übergeben
                    name = cursor.getString(artistCol),
                    albumCount = cursor.getInt(albumsCol),
                    trackCount = cursor.getInt(tracksCol)
                ))
            }
        }
        return artistList
    }

    fun loadAlbenForArtist(artistName: String): List<com.example.lmnt.model.Album> {
        val albumList = mutableListOf<com.example.lmnt.model.Album>()
        val uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Albums._ID,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.NUMBER_OF_SONGS
        )

        val selection = "${MediaStore.Audio.Albums.ARTIST} = ?"
        val selectionArgs = arrayOf(artistName)

        contentResolver.query(uri, projection, selection, selectionArgs, "${MediaStore.Audio.Albums.ALBUM} ASC")?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)
            val countCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val artworkUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"), id
                ).toString()

                albumList.add(com.example.lmnt.model.Album(
                    id,
                    cursor.getString(albumCol),
                    cursor.getString(artistCol),
                    artworkUri,
                    cursor.getInt(countCol)
                ))
            }
        }
        return albumList
    }

    fun loadSongsForArtistName(artistName: String): List<Song> {
        val songList = mutableListOf<Song>()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
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

        contentResolver.query(uri, projection, selection, selectionArgs, MediaStore.Audio.Media.TITLE + " ASC")?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)

            while (cursor.moveToNext()) {
                val albumId = cursor.getLong(albumIdCol)
                val artworkUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"), albumId
                ).toString()

                songList.add(Song(
                    id = cursor.getLong(idCol),
                    title = cursor.getString(titleCol),
                    artist = cursor.getString(artistCol),
                    uri = cursor.getString(dataCol),
                    artworkUri = artworkUri,
                    trackNumber = cursor.getInt(trackCol) % 1000,
                    discNumber = if (cursor.getInt(trackCol) >= 1000) cursor.getInt(trackCol) / 1000 else 1
                ))
            }
        }
        return songList
    }


    override fun onStart() {
        super.onStart()
        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()

        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()

                // --- WICHTIG: HIER WIRD DER MINIPLAYER AKTIVIERT ---
                setupMiniPlayer()

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Fehler beim Laden des Players", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onStop() {
        super.onStop()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
        mediaController = null
    }

    private fun setupMiniPlayer() {
        val titleTv = findViewById<TextView>(R.id.miniPlayerTitle)
        val artistTv = findViewById<TextView>(R.id.miniPlayerArtist)
        val albumArtIv = findViewById<ImageView>(R.id.miniPlayerAlbumArt)
        val playPauseBtn = findViewById<ImageButton>(R.id.btnMiniPlayerPlay)

        // 1. Initialer Check: Wenn schon Musik läuft, direkt anzeigen
        mediaController?.currentMediaItem?.mediaMetadata?.let { metadata ->
            titleTv.text = metadata.title ?: "Kein Titel"
            artistTv.text = metadata.artist ?: "Unbekannter Künstler"
            // Button Icon setzen
            val icon = if (mediaController?.isPlaying == true) R.drawable.player_pause
            else R.drawable.player_play
            playPauseBtn.setImageResource(icon)
        }

        // 2. Listener für Änderungen während der Laufzeit
        mediaController?.addListener(object : Player.Listener {
            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                titleTv.text = mediaMetadata.title ?: "Unbekannt"
                artistTv.text = mediaMetadata.artist ?: "Unbekannt"

                albumArtIv.load(mediaMetadata.artworkUri) {
                    crossfade(true) // Schöner Übergang
                    placeholder(R.drawable.ic_music_note) // Während es lädt
                    error(R.drawable.ic_music_note) // Falls kein Cover gefunden wurde
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                val icon = if (isPlaying) android.R.drawable.ic_media_pause
                else android.R.drawable.ic_media_play
                playPauseBtn.setImageResource(icon)
            }
        })

        // 3. Play/Pause Klick-Event
        playPauseBtn.setOnClickListener {
            mediaController?.let {
                if (it.isPlaying) it.pause() else it.play()
            }
        }
        val miniPlayerLayout = findViewById<LinearLayout>(R.id.miniPlayer)
        miniPlayerLayout.setOnClickListener {
            val playerFragment = PlayerFullscreenFragment()
            playerFragment.show(supportFragmentManager, "player")
        }
    }
}