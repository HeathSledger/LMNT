package com.example.lmnt.service

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
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

    private var startTime: Long = 0
    private var currentMediaItem: MediaItem? = null
    private var fadeJob: Job? = null

    // --- FADE LOGIK ---
    private fun fadeVolume(targetVolume: Float, durationMs: Long) {
        fadeJob?.cancel()
        fadeJob = serviceScope.launch {
            val startVolume = player.volume
            val steps = 20
            val interval = durationMs / steps
            val volumeStep = (targetVolume - startVolume) / steps

            for (i in 1..steps) {
                delay(interval)
                player.volume = startVolume + (i * volumeStep)
            }
            player.volume = targetVolume
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            saveCurrentTracking()
            currentMediaItem = mediaItem

            // Sanftes Einblenden
            player.volume = 0f
            fadeVolume(1f, 800L)

            if (player.isPlaying) {
                startTime = System.currentTimeMillis()
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                startTime = System.currentTimeMillis()
                currentMediaItem = player.currentMediaItem
            } else {
                saveCurrentTracking()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED || playbackState == Player.STATE_IDLE) {
                saveCurrentTracking()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Audio Attributes für Audio Focus & Bluetooth
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true) // Fokus-Handling
            .setHandleAudioBecomingNoisy(true)         // Pause bei Kopfhörer-Trennung
            .build()

        player.addListener(playerListener)

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("OPEN_PLAYER", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .build()
    }

    // WICHTIG: Verhindert, dass der Player bei Pause aus der Notification verschwindet
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        saveCurrentTracking()
        serviceScope.cancel()
        mediaSession?.run {
            player.removeListener(playerListener)
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    private fun saveCurrentTracking() {
        val item = currentMediaItem ?: return
        val start = startTime
        if (start == 0L) return
        val listenedMs = System.currentTimeMillis() - start

        // Wir tracken nur, wenn mindestens 2 Sekunden gehört wurden
        if (listenedMs > 2000L) {
            val calendar = Calendar.getInstance()
            val db = AppDatabase.getDatabase(this@PlaybackService)
            val songId = item.mediaId.toLongOrNull() ?: 0L

            val historyEntry = PlaybackHistory(
                songId = songId,
                songTitle = item.mediaMetadata.title?.toString() ?: "Unknown",
                artist = item.mediaMetadata.artist?.toString() ?: "Unknown",
                album = item.mediaMetadata.albumTitle?.toString() ?: "Unknown",
                duration = listenedMs,
                timestamp = System.currentTimeMillis(),
                year = calendar.get(Calendar.YEAR),
                month = calendar.get(Calendar.MONTH) + 1,
                day = calendar.get(Calendar.DAY_OF_MONTH)
            )
            serviceScope.launch(Dispatchers.IO) {
                db.historyDao().insert(historyEntry)
                db.historyDao().updateLastPlayed(songId, System.currentTimeMillis())
            }
        }
        startTime = 0L
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        // Nur stoppen, wenn nicht mehr abgespielt wird
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }
}