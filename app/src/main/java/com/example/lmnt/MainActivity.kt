package com.example.lmnt

import android.content.ComponentName
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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