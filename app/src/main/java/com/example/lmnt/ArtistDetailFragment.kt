package com.example.lmnt.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lmnt.MainActivity
import com.example.lmnt.MusicLoader
import com.example.lmnt.R
import com.example.lmnt.adapter.AlbumAdapter
import com.example.lmnt.SongsAdapter
import com.example.lmnt.Song

class ArtistDetailFragment : Fragment(R.layout.fragment_artist_detail) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val artistName = arguments?.getString("artistName") ?: ""
        view.findViewById<TextView>(R.id.tvArtistDetailName).text = artistName

        val rvAlben = view.findViewById<RecyclerView>(R.id.rvArtistAlbums)
        val rvSongs = view.findViewById<RecyclerView>(R.id.rvArtistSongs)

        // Buttons für "Alle Abspielen" und "Shuffle"
        val btnPlayAll = view.findViewById<ImageButton>(R.id.btnArtistPlayAll)
        val btnShuffle = view.findViewById<ImageButton>(R.id.btnArtistShuffle)

        val mainActivity = (activity as? MainActivity)

        // 1. Daten laden
        val alben = MusicLoader.loadAlbenForArtist(requireContext().contentResolver, artistName)
        val songs = MusicLoader.loadSongsForArtistName(requireContext().contentResolver, artistName)

        // 2. Alben Setup (Horizontal)
        rvAlben.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        rvAlben.adapter = AlbumAdapter(alben) { album ->
            val albumDetailFragment = AlbumDetailFragment.newInstance(album.id, album.artworkUri)
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, albumDetailFragment)
                .addToBackStack(null)
                .commit()
        }

        // 3. Songs Setup (Vertikal)
        rvSongs.layoutManager = LinearLayoutManager(context)
        rvSongs.adapter = SongsAdapter(
            songs = songs,
            showTrackNumber = true,
            onClick = { index ->
                mainActivity?.playPlaylist(ArrayList(songs), index)
            },
            onLongClick = { song ->
                mainActivity?.showSongOptions(song)
            }
        )

        // 4. "Alle abspielen" Logik
        btnPlayAll?.setOnClickListener {
            if (songs.isNotEmpty()) {
                mainActivity?.playPlaylist(ArrayList(songs), 0)
            }
        }

        // 5. "Shuffle" Logik
        btnShuffle?.setOnClickListener {
            if (songs.isNotEmpty()) {
                val shuffledSongs = ArrayList(songs)
                shuffledSongs.shuffle()
                mainActivity?.playPlaylist(shuffledSongs, 0)
            }
        }
    }

    companion object {
        fun newInstance(name: String) = ArtistDetailFragment().apply {
            arguments = Bundle().apply {
                putString("artistName", name)
            }
        }
    }
}