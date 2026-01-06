package com.example.lmnt

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.viewpager2.widget.ViewPager2
import coil.load
import com.example.lmnt.database.AppDatabase
import com.example.lmnt.database.PlaylistSongCrossRef
import com.example.lmnt.database.SongMetadata
import com.example.lmnt.service.PlaybackService
import com.example.lmnt.ui.AlbumsFragment
import com.example.lmnt.ui.ArtistsFragment
import com.example.lmnt.ui.MenuHubFragment
import com.example.lmnt.ui.theme.*
import com.example.lmnt.viewmodel.MusicViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var myAdapter: ViewPagerAdapter
    private lateinit var musicViewModel: MusicViewModel
    private var shouldOpenPlayerOnConnect = false

    private var controllerFuture: ListenableFuture<MediaController>? = null
    var mediaController: MediaController? = null

    private val playerListener = object : Player.Listener {
        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) = updateMiniPlayerUI()
        override fun onIsPlayingChanged(isPlaying: Boolean) = updateMiniPlayerUI()
        override fun onPlaybackStateChanged(state: Int) {
            val miniPlayerLayout = findViewById<LinearLayout>(R.id.miniPlayer)
            if (state == Player.STATE_IDLE) miniPlayerLayout.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        musicViewModel = ViewModelProvider(this).get(MusicViewModel::class.java)
        viewPager = findViewById(R.id.viewPager)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        setupViewPager()
        setupToolbarActions()
        checkPermissionsAndLoadData()

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val fragmentContainer = findViewById<View>(R.id.fragment_container)
                val searchView = findViewById<SearchView>(R.id.customSearchView)
                if (searchView.visibility == View.VISIBLE) {
                    closeSearchAndResetFilter()
                } else if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                    if (supportFragmentManager.backStackEntryCount <= 0) fragmentContainer.visibility = View.GONE
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })
    }

    private fun setupToolbarActions() {
        val btnSettings = findViewById<ImageButton>(R.id.btnSettings)
        val btnMore = findViewById<ImageButton>(R.id.btnMore)
        val btnSearch = findViewById<ImageButton>(R.id.btnSearch)
        val normalLayout = findViewById<LinearLayout>(R.id.normalToolbarLayout)
        val customSearchView = findViewById<SearchView>(R.id.customSearchView)

        btnSettings.setOnClickListener { MenuHubFragment().show(supportFragmentManager, "MenuHub") }
        btnMore.setOnClickListener { setupPopupMenu(it) }
        btnSearch.setOnClickListener {
            normalLayout.visibility = View.GONE
            customSearchView.visibility = View.VISIBLE
            customSearchView.isIconified = false
            customSearchView.requestFocus()
        }
        customSearchView.setOnCloseListener { closeSearchAndResetFilter(); false }
        customSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                musicViewModel.filterAll(newText ?: "")
                return true
            }
        })
    }

    private fun setupPopupMenu(view: View) {
        val popup = androidx.appcompat.widget.PopupMenu(this, view)
        when (viewPager.currentItem) {
            1 -> popup.menuInflater.inflate(R.menu.menu_songs, popup.menu)
            2 -> popup.menuInflater.inflate(R.menu.menu_albums, popup.menu)
            3 -> popup.menuInflater.inflate(R.menu.menu_artists, popup.menu)
        }
        popup.show()
    }

    private fun closeSearchAndResetFilter() {
        findViewById<SearchView>(R.id.customSearchView).apply {
            setQuery("", false)
            visibility = View.GONE
        }
        findViewById<LinearLayout>(R.id.normalToolbarLayout).visibility = View.VISIBLE
        musicViewModel.filterAll("")
    }

    private fun checkPermissionsAndLoadData() {
        val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_AUDIO
        } else android.Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            loadInitialData()
        } else ActivityCompat.requestPermissions(this, arrayOf(permission), 100)
    }

    private fun loadInitialData() {
        val allSongs = MusicLoader.loadAllSongs(contentResolver)
        musicViewModel.setSongs(allSongs)
        musicViewModel.setAlbums(MusicLoader.loadAlbums(contentResolver))
        musicViewModel.setArtists(MusicLoader.loadArtists(contentResolver))

        val db = AppDatabase.getDatabase(this)
        lifecycleScope.launch(Dispatchers.IO) {
            val metaList = allSongs.map { SongMetadata(songId = it.id) }
            db.historyDao().insertInitialMetadataList(metaList)
        }
    }

    private fun setupViewPager() {
        val fragments = listOf(HomeFragment(), SongsFragment(), AlbumsFragment(), ArtistsFragment(), PlaylistsFragment())
        myAdapter = ViewPagerAdapter(this, fragments)
        viewPager.adapter = myAdapter
        viewPager.isUserInputEnabled = false
        bottomNavigation.setOnItemSelectedListener { item ->
            closeSearchAndResetFilter()
            viewPager.currentItem = when (item.itemId) {
                R.id.nav_home -> 0
                R.id.nav_songs -> 1
                R.id.nav_albums -> 2
                R.id.nav_artists -> 3
                R.id.nav_playlists -> 4
                else -> 0
            }
            true
        }
    }

    // --- SONG OPTIONS MENU ---
    fun showSongOptions(song: Song) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_song_options, null)

        val layoutFavorite = view.findViewById<LinearLayout>(R.id.layoutFavorite)
        val layoutNext = view.findViewById<LinearLayout>(R.id.layoutNext)
        val layoutQueue = view.findViewById<LinearLayout>(R.id.layoutQueue)
        val layoutPlaylist = view.findViewById<LinearLayout>(R.id.layoutPlaylist)
        val layoutDelete = view.findViewById<LinearLayout>(R.id.layoutDelete)
        val heartIcon = view.findViewById<ImageView>(R.id.optFavorite)
        val favoriteText = view.findViewById<TextView>(R.id.tvFavoriteText)

        val db = AppDatabase.getDatabase(this)

        fun updateFavoriteUI(isFav: Boolean) {
            if (isFav) {
                heartIcon.setImageResource(R.drawable.heart_filled)
                heartIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#1DB954"))
                favoriteText.text = "Aus Favoriten entfernen"
                favoriteText.setTextColor(Color.parseColor("#1DB954"))
            } else {
                heartIcon.setImageResource(R.drawable.heart)
                heartIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#B3B3B3"))
                favoriteText.text = "Zu Favoriten hinzufügen"
                favoriteText.setTextColor(Color.parseColor("#B3B3B3"))
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val isFav = db.historyDao().isFavorite(song.id)
            withContext(Dispatchers.Main) { updateFavoriteUI(isFav) }
        }

        layoutFavorite.setOnClickListener {
            animateHeart(heartIcon)
            lifecycleScope.launch(Dispatchers.IO) {
                val current = db.historyDao().isFavorite(song.id)
                db.historyDao().setFavorite(song.id, !current)
                withContext(Dispatchers.Main) {
                    updateFavoriteUI(!current)
                    musicViewModel.refreshFavorites()
                }
            }
        }

        layoutNext.setOnClickListener {
            mediaController?.addMediaItem(mediaController?.currentMediaItemIndex?.plus(1) ?: 0, createMediaItem(song))
            bottomSheetDialog.dismiss()
            Toast.makeText(this, "Wird als nächstes gespielt", Toast.LENGTH_SHORT).show()
        }

        layoutQueue.setOnClickListener {
            mediaController?.addMediaItem(createMediaItem(song))
            bottomSheetDialog.dismiss()
            Toast.makeText(this, "An Warteschlange angehängt", Toast.LENGTH_SHORT).show()
        }

        layoutPlaylist.setOnClickListener {
            bottomSheetDialog.dismiss()
            showAddToPlaylistDialog(song)
        }

        layoutDelete.setOnClickListener {
            bottomSheetDialog.dismiss()
            AlertDialog.Builder(this)
                .setTitle("Song löschen?")
                .setMessage("Möchtest du '${song.title}' wirklich löschen?")
                .setPositiveButton("Löschen") { _, _ -> deleteSong(song) }
                .setNegativeButton("Abbrechen", null)
                .show()
        }

        view.findViewById<TextView>(R.id.menuSongTitle).text = song.title
        view.findViewById<TextView>(R.id.menuArtistName).text = song.artist
        view.findViewById<ImageView>(R.id.menuAlbumArt).load(song.artworkUri)

        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.show()
    }

    private fun showAddToPlaylistDialog(song: Song) {
        val db = AppDatabase.getDatabase(this)
        lifecycleScope.launch(Dispatchers.IO) {
            val playlists = db.playlistDao().getAllPlaylistsStatic()
            withContext(Dispatchers.Main) {
                if (playlists.isEmpty()) {
                    Toast.makeText(this@MainActivity, "Keine Playlists vorhanden", Toast.LENGTH_SHORT).show()
                    return@withContext
                }
                val names = playlists.map { it.name }.toTypedArray()
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Zu Playlist hinzufügen")
                    .setItems(names) { _, which ->
                        val pId = playlists[which].id
                        lifecycleScope.launch(Dispatchers.IO) {
                            db.playlistDao().insertSongToPlaylist(PlaylistSongCrossRef(pId, song.id))
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@MainActivity, "Hinzugefügt", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .show()
            }
        }
    }

    private fun deleteSong(song: Song) {
        // Logik zur DB-Entfernung oder File-Löschung hier
        Toast.makeText(this, "${song.title} gelöscht", Toast.LENGTH_SHORT).show()
    }

    private fun createMediaItem(song: Song) = MediaItem.Builder()
        .setMediaId(song.id.toString())
        .setUri(Uri.parse(song.uri))
        .setMediaMetadata(MediaMetadata.Builder()
            .setTitle(song.title).setArtist(song.artist)
            .setArtworkUri(Uri.parse(song.artworkUri)).build())
        .build()

    // --- MEDIA CONTROLLER LIFECYCLE ---
    override fun onStart() {
        super.onStart()
        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture?.addListener({
            mediaController = controllerFuture?.get()?.apply { addListener(playerListener) }
            setupMiniPlayer()
            if (shouldOpenPlayerOnConnect) { openFullscreen(); shouldOpenPlayerOnConnect = false }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onStop() {
        super.onStop()
        mediaController?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
    }

    private fun setupMiniPlayer() {
        val miniPlayerLayout = findViewById<LinearLayout>(R.id.miniPlayer)
        updateMiniPlayerUI()

        findViewById<ImageButton>(R.id.btnMiniPlayerPlay).setOnClickListener {
            mediaController?.let { if (it.isPlaying) it.pause() else it.play() }
        }
        findViewById<ImageButton>(R.id.btnMiniPlayerNext).setOnClickListener { mediaController?.seekToNext() }
        findViewById<ImageButton>(R.id.btnMiniPlayerPrev).setOnClickListener { mediaController?.seekToPrevious() }
        findViewById<ImageButton>(R.id.btnMiniPlayerClose).setOnClickListener {
            mediaController?.stop()
            mediaController?.clearMediaItems()
            miniPlayerLayout.visibility = View.GONE
        }
        miniPlayerLayout.setOnClickListener { openFullscreen() }
    }

    fun playPlaylist(songs: ArrayList<Song>, startIndex: Int) {
        val controller = mediaController ?: return
        val mediaItems = songs.map { createMediaItem(it) }
        controller.setMediaItems(mediaItems, startIndex, 0)
        controller.prepare()
        controller.play()
    }

    fun showFragment(fragment: Fragment) {
        findViewById<View>(R.id.fragment_container).visibility = View.VISIBLE
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun updateMiniPlayerUI() {
        val controller = mediaController ?: return
        val miniPlayerLayout = findViewById<LinearLayout>(R.id.miniPlayer)
        val metadata = controller.mediaMetadata
        if (metadata.title != null) {
            miniPlayerLayout.visibility = View.VISIBLE
            findViewById<TextView>(R.id.miniPlayerTitle).text = metadata.title
            findViewById<TextView>(R.id.miniPlayerArtist).text = metadata.artist
            findViewById<ImageView>(R.id.miniPlayerAlbumArt).load(metadata.artworkUri)
            findViewById<ImageButton>(R.id.btnMiniPlayerPlay).setImageResource(
                if (controller.isPlaying) R.drawable.player_pause else R.drawable.player_play
            )
        } else miniPlayerLayout.visibility = View.GONE
    }

    private fun animateHeart(view: View) {
        view.animate().scaleX(1.3f).scaleY(1.3f).setDuration(100).withEndAction {
            view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
        }.start()
    }

    private fun openFullscreen() = PlayerFullscreenFragment().show(supportFragmentManager, "player")
}