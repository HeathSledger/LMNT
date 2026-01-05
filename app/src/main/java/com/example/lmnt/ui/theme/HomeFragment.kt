package com.example.lmnt.ui.theme

import android.content.ComponentName
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lmnt.MainActivity
import com.example.lmnt.MusicLoader
import com.example.lmnt.R
import com.example.lmnt.adapter.RecentlyAddedAdapter
import com.example.lmnt.service.PlaybackService
import com.google.android.material.card.MaterialCardView
import com.google.common.util.concurrent.MoreExecutors
import java.util.Calendar

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var mediaController: MediaController? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Begrüßung basierend auf der Uhrzeit anpassen
        val tvGreeting = view.findViewById<TextView>(R.id.tvGreeting)
        tvGreeting.text = getGreeting()

        // 2. RecyclerView für "Zuletzt hinzugefügt" einrichten
        val rvRecentlyAdded = view.findViewById<RecyclerView>(R.id.rvRecentlyAdded)

        // Horizontaler Scroll-Modus
        rvRecentlyAdded.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        // Daten laden (mit dem Limit, das wir besprochen hatten, z.B. 15)
        val recentSongs = MusicLoader.loadRecentlyAdded(requireContext().contentResolver, limit = 50)

        // Adapter setzen
        rvRecentlyAdded.adapter = RecentlyAddedAdapter(recentSongs) { song ->
            // Beim Klick auf einen Song: Playlist mit diesem Song starten
            (activity as? MainActivity)?.playPlaylist(listOf(song), 0)
        }

        // 3. Shuffle Button Logik
        view.findViewById<View>(R.id.btnRandomPlay).setOnClickListener {
            val allSongs = MusicLoader.loadAllSongs(requireContext().contentResolver)
            if (allSongs.isNotEmpty()) {
                val shuffled = allSongs.shuffled()
                (activity as? MainActivity)?.playPlaylist(shuffled, 0)
            }
        }
    }

    private fun playRandomMusic() {
        val controller = mediaController ?: run {
            Toast.makeText(requireContext(), "Player nicht bereit", Toast.LENGTH_SHORT).show()
            return
        }

        // Nutze deinen MusicLoader, um echte Songs zu laden
        val songs = MusicLoader.loadAllSongs(requireContext().contentResolver)

        if (songs.isNotEmpty()) {
            // Liste mischen und in MediaItems konvertieren
            val mediaItems = songs.shuffled().map { song ->
                MediaItem.Builder()
                    .setMediaId(song.id.toString())
                    .setUri(song.uri)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(song.title)
                            .setArtist(song.artist)
                            .setAlbumTitle(song.album)
                            .setArtworkUri(android.net.Uri.parse(song.artworkUri))
                            .build()
                    )
                    .build()
            }

            controller.setMediaItems(mediaItems)
            controller.prepare()
            controller.play()

            Toast.makeText(requireContext(), "Playing shuffled music", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Keine Musik gefunden!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playSong(song: com.example.lmnt.Song) {
        val controller = mediaController ?: return

        val mediaItem = MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(song.uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .setArtworkUri(android.net.Uri.parse(song.artworkUri))
                    .build()
            )
            .build()

        controller.setMediaItem(mediaItem)
        controller.prepare()
        controller.play()
    }




    private fun getGreeting(): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Good Morning"
            in 12..17 -> "Good Afternoon"
            in 18..22 -> "Good Evening"
            else -> "Good Night"
        }
    }
}