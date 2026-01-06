package com.example.lmnt.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.lmnt.MainActivity
import com.example.lmnt.MusicLoader
import com.example.lmnt.R
import com.example.lmnt.SongsAdapter
import com.example.lmnt.Song

class AlbumDetailFragment : Fragment(R.layout.fragment_album_detail) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Daten aus den Arguments holen - WICHTIG: Key muss "artworkUri" sein!
        val albumId = arguments?.getLong("albumId") ?: -1L
        val albumArt = arguments?.getString("artworkUri") // KEY KORRIGIERT

        val imageView = view.findViewById<ImageView>(R.id.detailAlbumArt)
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvAlbumSongs)

        // 2. Cover laden mit Glide
        Glide.with(this)
            .load(albumArt)
            .placeholder(R.drawable.ic_music_note)
            .error(R.drawable.ic_music_note)
            .centerCrop()
            .into(imageView)

        // 3. Daten über die MainActivity laden
        val mainActivity = (activity as? MainActivity)
        val songs = MusicLoader.loadSongsForAlbum(requireContext().contentResolver, albumId)

        // Sortierung nach Disc und dann Tracknummer
        val sortedSongs = songs.sortedWith(compareBy({ it.discNumber }, { it.trackNumber }))

        // 4. RecyclerView Setup
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Nutze benannte Argumente, um Verwechslungen mit dem neuen onLongClick zu vermeiden
        recyclerView.adapter = SongsAdapter(
            songs = sortedSongs,
            showTrackNumber = true,
            onClick = { index ->
                mainActivity?.playPlaylist(ArrayList(sortedSongs), index)
            },
            onLongClick = { song ->
                mainActivity?.showSongOptions(song)
            }
        )
    }

    companion object {
        fun newInstance(albumId: Long, albumArt: String?) = AlbumDetailFragment().apply {
            arguments = Bundle().apply {
                putLong("albumId", albumId)
                putString("artworkUri", albumArt) // Dieser Key muss oben beim Auslesen exakt gleich sein
            }
        }
    }
}