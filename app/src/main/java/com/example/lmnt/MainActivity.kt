package com.example.lmnt

import android.content.ComponentName
import android.content.Intent
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
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.viewpager2.widget.ViewPager2
import coil.load
import com.example.lmnt.database.AppDatabase
import com.example.lmnt.database.SongMetadata
import com.example.lmnt.service.PlaybackService
import com.example.lmnt.ui.AlbumsFragment
import com.example.lmnt.ui.ArtistsFragment
import com.example.lmnt.ui.MenuHubFragment
import com.example.lmnt.ui.theme.*
import com.example.lmnt.viewmodel.MusicViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var myAdapter: ViewPagerAdapter
    private lateinit var musicViewModel: MusicViewModel
    private var shouldOpenPlayerOnConnect = false

    private var controllerFuture: ListenableFuture<MediaController>? = null
    var mediaController: MediaController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // --- Toolbar & Settings ---
        val btnSettings = findViewById<ImageButton>(R.id.btnSettings)
        btnSettings.setOnClickListener {
            MenuHubFragment().show(supportFragmentManager, "MenuHub")
        }

        val btnMore = findViewById<ImageButton>(R.id.btnMore)
        btnMore.setOnClickListener { view ->
            setupPopupMenu(view)
        }

        // --- ViewModel & UI ---
        musicViewModel = ViewModelProvider(this).get(MusicViewModel::class.java)
        viewPager = findViewById(R.id.viewPager)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        val normalLayout = findViewById<LinearLayout>(R.id.normalToolbarLayout)
        val customSearchView = findViewById<SearchView>(R.id.customSearchView)
        val btnSearch = findViewById<ImageButton>(R.id.btnSearch)

        setupViewPager()
        checkPermissionsAndLoadData()

        // --- Search Logic ---
        btnSearch.setOnClickListener {
            normalLayout.visibility = View.GONE
            customSearchView.visibility = View.VISIBLE
            customSearchView.isIconified = false
            customSearchView.requestFocus()
        }

        customSearchView.setOnCloseListener {
            closeSearchAndResetFilter()
            false
        }

        customSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                musicViewModel.filterAll(newText ?: "")
                return true
            }
        })



        // --- Back Button Handling ---
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val fragmentContainer = findViewById<View>(R.id.fragment_container)
                val searchView = findViewById<SearchView>(R.id.customSearchView)

                if (searchView.visibility == View.VISIBLE) {
                    closeSearchAndResetFilter()
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

    private fun setupPopupMenu(view: View) {
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
            2 -> { // Albums Tab
                popup.menuInflater.inflate(R.menu.menu_albums, popup.menu)
                popup.setOnMenuItemClickListener { item ->
                    val fragment = myAdapter.getFragment(2) as? AlbumsFragment
                    when (item.itemId) {
                        R.id.grid_2 -> { saveGridSetting("albums_grid", 2); fragment?.changeGridColumns(2); true }
                        R.id.grid_3 -> { saveGridSetting("albums_grid", 3); fragment?.changeGridColumns(3); true }
                        R.id.grid_4 -> { saveGridSetting("albums_grid", 4); fragment?.changeGridColumns(4); true }
                        else -> false
                    }
                }
            }
            3 -> { // Artists Tab
                popup.menuInflater.inflate(R.menu.menu_artists, popup.menu)
                popup.setOnMenuItemClickListener { item ->
                    if (item.itemId == R.id.action_toggle_view) {
                        val fragment = myAdapter.getFragment(3) as? ArtistsFragment
                        val isNowList = fragment?.toggleViewMode() ?: false
                        getSharedPreferences("LMNT_Settings", MODE_PRIVATE).edit().putBoolean("artists_is_list", isNowList).apply()
                        true
                    } else false
                }
            }
        }
        popup.show()
    }

    private fun closeSearchAndResetFilter() {
        val normalLayout = findViewById<LinearLayout>(R.id.normalToolbarLayout)
        val customSearchView = findViewById<SearchView>(R.id.customSearchView)
        if (customSearchView.visibility == View.VISIBLE) {
            customSearchView.setQuery("", false)
            musicViewModel.filterAll("")
            customSearchView.visibility = View.GONE
            normalLayout.visibility = View.VISIBLE
        }
    }

    private fun saveGridSetting(key: String, value: Int) {
        getSharedPreferences("LMNT_Settings", MODE_PRIVATE).edit().putInt(key, value).apply()
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

    override fun onResume() {
        super.onResume()
        if (intent.getBooleanExtra("OPEN_PLAYER", false)) {
            intent.removeExtra("OPEN_PLAYER")
            if (mediaController != null) openFullscreen() else shouldOpenPlayerOnConnect = true
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun loadInitialData() {
        val allSongs = MusicLoader.loadAllSongs(contentResolver)
        musicViewModel.setSongs(allSongs)
        musicViewModel.setAlbums(MusicLoader.loadAlbums(contentResolver))
        musicViewModel.setArtists(MusicLoader.loadArtists(contentResolver))

        val db = AppDatabase.getDatabase(this)
        lifecycleScope.launch(Dispatchers.IO) {
            allSongs.forEach { song ->
                db.historyDao().insertInitialMetadata(SongMetadata(songId = song.id))
            }
        }
    }

    // Diese Funktion sorgt dafür, dass Detail-Fragmente (wie History)
    // den Container einblenden und korrekt angezeigt werden.
    fun showFragment(fragment: androidx.fragment.app.Fragment) {
        val container = findViewById<View>(R.id.fragment_container)
        container.visibility = View.VISIBLE // Macht den Container für den User sichtbar

        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out) // Schicker Übergang
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null) // Ermöglicht das Zurückkehren mit dem System-Back-Button
            .commit()
    }

    private fun setupViewPager() {
        val fragments = listOf(HomeFragment(), SongsFragment(), AlbumsFragment(), ArtistsFragment(), PlaylistsFragment())
        myAdapter = ViewPagerAdapter(this, fragments)
        viewPager.adapter = myAdapter
        viewPager.isUserInputEnabled = false

        bottomNavigation.setOnItemSelectedListener { item ->
            closeSearchAndResetFilter()
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

    fun playPlaylist(songs: List<Song>, startIndex: Int) {
        val controller = mediaController ?: return
        val mediaItems = songs.map { song ->
            MediaItem.Builder()
                .setMediaId(song.id.toString())
                .setUri(Uri.parse(song.uri))
                .setMediaMetadata(MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .setArtworkUri(Uri.parse(song.artworkUri))
                    .build())
                .build()
        }
        controller.setMediaItems(mediaItems, startIndex, 0L)
        controller.prepare()
        controller.play()
    }

    override fun onStart() {
        super.onStart()
        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                setupMiniPlayer()
                if (shouldOpenPlayerOnConnect) {
                    openFullscreen()
                    shouldOpenPlayerOnConnect = false
                }
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

        // --- NEUE BUTTONS ---
        val prevBtn = findViewById<ImageButton>(R.id.btnMiniPlayerPrev)
        val nextBtn = findViewById<ImageButton>(R.id.btnMiniPlayerNext)
        val closeBtn = findViewById<ImageButton>(R.id.btnMiniPlayerClose)

        fun updateUI(controller: Player) {
            val metadata = controller.mediaMetadata
            if (metadata.title != null) {
                miniPlayerLayout.visibility = View.VISIBLE
                titleTv.text = metadata.title
                artistTv.text = metadata.artist
                albumArtIv.load(metadata.artworkUri) {
                    placeholder(R.drawable.ic_music_note)
                    error(R.drawable.ic_music_note)
                }
                playPauseBtn.setImageResource(if (controller.isPlaying) R.drawable.player_pause else R.drawable.player_play)
            } else {
                miniPlayerLayout.visibility = View.GONE
            }
        }

        mediaController?.let { updateUI(it) }

        mediaController?.addListener(object : Player.Listener {
            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) = updateUI(mediaController!!)
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playPauseBtn.setImageResource(if (isPlaying) R.drawable.player_pause else R.drawable.player_play)
                if (isPlaying) miniPlayerLayout.visibility = View.VISIBLE
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_IDLE) miniPlayerLayout.visibility = View.GONE
            }
        })

        // --- LISTENERS FÜR NEUE BUTTONS ---
        playPauseBtn.setOnClickListener {
            mediaController?.let { if (it.isPlaying) it.pause() else it.play() }
        }

        nextBtn.setOnClickListener {
            mediaController?.seekToNext()
        }

        prevBtn.setOnClickListener {
            mediaController?.seekToPrevious()
        }

        closeBtn.setOnClickListener {
            mediaController?.stop()
            mediaController?.clearMediaItems()
            miniPlayerLayout.visibility = View.GONE
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