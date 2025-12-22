package com.example.lmnt.service

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.lmnt.MainActivity
import com.example.lmnt.database.AppDatabase
import com.example.lmnt.database.PlaybackHistory
import kotlinx.coroutines.*
import java.util.Calendar

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Tracking Variablen
    private var startTime: Long = 0
    private var currentMediaItem: MediaItem? = null

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            // 1. Speichere die Zeit des GERADE BEENDETEN Songs
            saveCurrentTracking()

            // 2. Starte das Tracking für den NEUEN Song
            currentMediaItem = mediaItem
            if (player.isPlaying) {
                startTime = System.currentTimeMillis()
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                // Musik wurde gestartet oder nach Pause fortgesetzt
                startTime = System.currentTimeMillis()
                currentMediaItem = player.currentMediaItem
            } else {
                // Musik wurde pausiert oder gestoppt
                saveCurrentTracking()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED || playbackState == Player.STATE_IDLE) {
                saveCurrentTracking()
            }
        }
    }

    private fun saveCurrentTracking() {
        val item = currentMediaItem ?: return
        val start = startTime

        // Wenn startTime 0 ist, lief kein Tracking
        if (start == 0L) return

        val listenedMs = System.currentTimeMillis() - start

        // Wir speichern erst ab 2 Sekunden, um "Fehlspeicherungen" beim schnellen Skippen zu vermeiden
        if (listenedMs > 2000L) {
            val calendar = Calendar.getInstance()
            val db = AppDatabase.getDatabase(this@PlaybackService)

            val historyEntry = PlaybackHistory(
                songId = item.mediaId.toLongOrNull() ?: 0L,
                songTitle = item.mediaMetadata.title?.toString() ?: "Unknown",
                artist = item.mediaMetadata.artist?.toString() ?: "Unknown",
                album = item.mediaMetadata.albumTitle?.toString() ?: "Unknown", // Wichtig für Top-Alben!
                duration = listenedMs, // Hier speichern wir die TATSÄCHLICHE Zeit
                timestamp = System.currentTimeMillis(),
                year = calendar.get(Calendar.YEAR),
                month = calendar.get(Calendar.MONTH) + 1,
                day = calendar.get(Calendar.DAY_OF_MONTH)
            )

            serviceScope.launch(Dispatchers.IO) {
                db.historyDao().insert(historyEntry)
            }
        }

        // Timer zurücksetzen, damit nicht doppelt gerechnet wird
        startTime = 0L
    }

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()
        player.addListener(playerListener)

        // Das Intent, das die MainActivity öffnet
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("OPEN_PLAYER", true) // Markierung setzen
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // MediaSession mit dem PendingIntent verknüpfen
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent) // DAS ermöglicht den Klick auf die Notification
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        saveCurrentTracking() // Letzte Zeit sichern beim Schließen
        serviceScope.cancel()
        mediaSession?.run {
            player.removeListener(playerListener)
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        // Nur stoppen, wenn NICHTS spielt.
        // Wenn player?.isPlaying true ist, NICHT stopSelf() aufrufen!
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }
}