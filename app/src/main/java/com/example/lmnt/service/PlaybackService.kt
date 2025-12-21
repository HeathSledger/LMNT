package com.example.lmnt.service

import android.content.Intent
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.lmnt.database.AppDatabase
import com.example.lmnt.database.PlaybackHistory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    // Das ist das Gehirn: Hier hören wir zu, wenn Songs gewechselt werden
    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            // Wir loggen den Song in die Statistik-Datenbank
            mediaItem?.let { item ->
                saveSongToHistory(item.mediaId.toLong())
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        // 1. Player initialisieren
        player = ExoPlayer.Builder(this).build()
        player.addListener(playerListener)

        // 2. MediaSession erstellen (Verbindung zur Außenwelt/Notification)
        mediaSession = MediaSession.Builder(this, player).build()
    }

    // Task für das Speichern in die Room-Datenbank
    private fun saveSongToHistory(songId: Long) {
        val calendar = Calendar.getInstance()
        val historyEntry = PlaybackHistory(
            songId = songId,
            year = calendar.get(Calendar.YEAR),
            month = calendar.get(Calendar.MONTH) + 1, // Jan ist 0
            timestamp = System.currentTimeMillis()
        )

        serviceScope.launch {
            try {
                val db = AppDatabase.getDatabase(this@PlaybackService)
                db.historyDao().insert(historyEntry)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Notwendig, damit das System weiß, welche Session aktiv ist
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    // Aufräumen, wenn der Service beendet wird
    override fun onDestroy() {
        mediaSession?.run {
            player.removeListener(playerListener)
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    // Wichtig für Android 12+ (Zufällige Abstürze verhindern bei Tasks im Hintergrund)
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }
}