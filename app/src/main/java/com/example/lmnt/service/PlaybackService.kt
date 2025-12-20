package com.example.lmnt.service

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.lmnt.MainActivity

class PlaybackService : MediaSessionService() {


    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer

    override fun onCreate() {
        super.onCreate()

        // 1. ExoPlayer initialisieren
        player = ExoPlayer.Builder(this).build()

        // 2. PendingIntent erstellen: Öffnet die App, wenn man auf die Notification klickt
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 3. MediaSession bauen und mit dem Player verknüpfen
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent) // Wichtig für die Rückkehr zur App
            .build()
    }

    // Ermöglicht dem System (Sperrbildschirm/Benachrichtigung) den Zugriff auf die Steuerung
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    // Wichtig: Ressourcen sauber freigeben, wenn der Service beendet wird
    override fun onDestroy() {
        mediaSession?.run {
            player.release() // Stoppt die Musik und gibt Speicher frei
            release()        // Gibt die Session frei
            mediaSession = null
        }
        super.onDestroy()
    }
}