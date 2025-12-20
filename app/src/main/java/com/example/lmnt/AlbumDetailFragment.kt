package com.example.lmnt.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.lmnt.MainActivity
import com.example.lmnt.R
import com.example.lmnt.SongsAdapter

class AlbumDetailFragment : Fragment(R.layout.fragment_album_detail) {

    private var albumId: Long = -1
    private var albumArt: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Keys aus den Arguments holen
        albumId = arguments?.getLong("albumId") ?: -1
        albumArt = arguments?.getString("albumArt")

        val imageView = view.findViewById<ImageView>(R.id.detailAlbumArt)
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvAlbumSongs)

        // 1. Cover laden
        Glide.with(this)
            .load(albumArt)
            .placeholder(R.drawable.ic_music_note)
            .centerCrop()
            .into(imageView)

        // 2. Daten laden & sortieren
        val mainActivity = (activity as? MainActivity)
        val songs = mainActivity?.loadSongsForAlbum(albumId) ?: emptyList()

        // Sortierung nach Disc und dann Tracknummer (wichtig für Alben!)
        val sortedSongs = songs.sortedWith(compareBy({ it.discNumber }, { it.trackNumber }))

        // 3. Adapter konfigurieren
        recyclerView.layoutManager = LinearLayoutManager(context)

        // HIER: showTrackNumber auf TRUE, damit im Album die Nummern erscheinen
        recyclerView.adapter = SongsAdapter(sortedSongs, showTrackNumber = true) { clickedSong ->
            val index = sortedSongs.indexOf(clickedSong)
            if (index != -1) {
                mainActivity?.playPlaylist(sortedSongs, index)
            }
        }


    }

    companion object {
        fun newInstance(albumId: Long, albumArt: String?) = AlbumDetailFragment().apply {
            arguments = Bundle().apply {
                putLong("albumId", albumId)
                putString("albumArt", albumArt)
            }
        }
    }
}