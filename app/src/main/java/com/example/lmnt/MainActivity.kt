package com.example.lmnt

import android.content.ComponentName
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.viewpager2.widget.ViewPager2
import coil.load
import com.example.lmnt.service.PlaybackService
import com.example.lmnt.ui.AlbumsFragment
import com.example.lmnt.ui.ArtistsFragment
import com.example.lmnt.ui.theme.*
import com.example.lmnt.viewmodel.MusicViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.common.util.concurrent.ListenableFuture
import com.example.lmnt.ui.MenuHubFragment


class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var myAdapter: ViewPagerAdapter
    private lateinit var musicViewModel: MusicViewModel

    private var controllerFuture: ListenableFuture<MediaController>? = null
    var mediaController: MediaController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val btnSettings = findViewById<ImageButton>(R.id.btnSettings) // Deine ID anpassen
        btnSettings.setOnClickListener {
            val menuHub = MenuHubFragment()
            menuHub.show(supportFragmentManager, "MenuHub")
        }

        // In der onCreate deiner MainActivity
        val btnMore = findViewById<ImageButton>(R.id.btnMore)

        btnMore.setOnClickListener { view ->
            val popup = androidx.appcompat.widget.PopupMenu(this, view)



            when (viewPager.currentItem) {
                1 -> { // Songs Tab
                    popup.menuInflater.inflate(R.menu.menu_songs, popup.menu)
                    popup.setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.sort_az -> { musicViewModel.sortSongs(true); true }
                            R.id.sort_za -> { musicViewModel.sortSongs(false); true }
                            else -> false
                        }
                    }
                }
                2 -> { // Alben Tab
                    popup.menuInflater.inflate(R.menu.menu_albums, popup.menu)
                    popup.setOnMenuItemClickListener { item ->
                        val fragment = myAdapter.getFragment(2) as? AlbumsFragment
                        when (item.itemId) {
                            R.id.grid_2 -> { fragment?.changeGridColumns(2); true }
                            R.id.grid_3 -> { fragment?.changeGridColumns(3); true }
                            R.id.grid_4 -> { fragment?.changeGridColumns(4); true }
                            else -> false
                        }
                    }
                }
                3 -> { // Artists Tab
                    popup.menuInflater.inflate(R.menu.menu_artists, popup.menu)
                    popup.setOnMenuItemClickListener { item ->
                        val fragment = myAdapter.getFragment(3) as? ArtistsFragment
                        if (item.itemId == R.id.action_toggle_view) {
                            fragment?.toggleViewMode()
                            true
                        } else false
                    }
                }

            } // Ende des when-Blocks

            popup.show()
        }

        // 1. ViewModel initialisieren
        musicViewModel = ViewModelProvider(this).get(MusicViewModel::class.java)

        // 2. UI Elemente finden
        viewPager = findViewById(R.id.viewPager)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        val normalLayout = findViewById<LinearLayout>(R.id.normalToolbarLayout)
        val customSearchView = findViewById<SearchView>(R.id.customSearchView)
        val btnSearch = findViewById<ImageButton>(R.id.btnSearch)

        // 3. ViewPager & Daten Setup
        setupViewPager()
        checkPermissionsAndLoadData()

        // 4. Such-Logik
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
                // Das ViewModel filtert jetzt zentral für alle Fragmente gleichzeitig!
                musicViewModel.filterAll(newText ?: "")
                return true
            }
        })

        // 5. Back Button Handling
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val fragmentContainer = findViewById<View>(R.id.fragment_container)
                val searchView = findViewById<SearchView>(R.id.customSearchView)
                val toolbar = findViewById<LinearLayout>(R.id.normalToolbarLayout)

                if (searchView.visibility == View.VISIBLE) {
                    searchView.visibility = View.GONE
                    toolbar.visibility = View.VISIBLE
                } else if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                    if (supportFragmentManager.backStackEntryCount <= 1) {
                        fragmentContainer.visibility = View.GONE
                    }
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }

            }

        })

    }

    private fun checkPermissionsAndLoadData() {
        val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_AUDIO
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            loadInitialData()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(permission), 100)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadInitialData()
        }
    }

    private fun loadInitialData() {
        musicViewModel.setSongs(MusicLoader.loadAllSongs(contentResolver))
        musicViewModel.setAlbums(MusicLoader.loadAlbums(contentResolver))
        musicViewModel.setArtists(MusicLoader.loadArtists(contentResolver))
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

        miniPlayerLayout.visibility = View.GONE

        mediaController?.addListener(object : Player.Listener {
            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                if (mediaMetadata.title != null) {
                    miniPlayerLayout.visibility = View.VISIBLE
                    titleTv.text = mediaMetadata.title
                    artistTv.text = mediaMetadata.artist
                    albumArtIv.load(mediaMetadata.artworkUri) {
                        placeholder(R.drawable.ic_music_note)
                        error(R.drawable.ic_music_note)
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playPauseBtn.setImageResource(if (isPlaying) R.drawable.player_pause else R.drawable.player_play)
                if (isPlaying) miniPlayerLayout.visibility = View.VISIBLE
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) {
                    miniPlayerLayout.visibility = View.GONE
                }
            }
        })

        playPauseBtn.setOnClickListener {
            mediaController?.let { if (it.isPlaying) it.pause() else it.play() }
        }

        miniPlayerLayout.setOnTouchListener(object : View.OnTouchListener {
            private var startY = 0f
            override fun onTouch(v: View, event: android.view.MotionEvent): Boolean {
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> { startY = event.y; return true }
                    android.view.MotionEvent.ACTION_UP -> {
                        val deltaY = startY - event.y
                        if (deltaY > 150 || Math.abs(deltaY) < 20) openFullscreen()
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun openFullscreen() {
        PlayerFullscreenFragment().show(supportFragmentManager, "player")
    }
}