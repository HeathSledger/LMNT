package com.example.lmnt.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lmnt.MainActivity
import com.example.lmnt.MusicLoader
import com.example.lmnt.R
import com.example.lmnt.adapter.AlbumAdapter
import com.example.lmnt.SongsAdapter
import com.example.lmnt.ui.AlbumDetailFragment

class ArtistDetailFragment : Fragment(R.layout.fragment_artist_detail) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val artistName = arguments?.getString("artistName") ?: ""
        view.findViewById<TextView>(R.id.tvArtistDetailName).text = artistName

        val rvAlben = view.findViewById<RecyclerView>(R.id.rvArtistAlbums)
        val rvSongs = view.findViewById<RecyclerView>(R.id.rvArtistSongs)

        val mainActivity = (activity as? MainActivity)

        // 1. Alben des Künstlers laden (Horizontal)
        val alben = MusicLoader.loadAlbenForArtist(requireContext().contentResolver, artistName)
        rvAlben.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        // ALBUM ADAPTER SETZEN
        rvAlben.adapter = AlbumAdapter(alben) { album ->
            // Öffnet das Album-Detail, wenn man auf ein Album des Künstlers klickt
            val albumDetailFragment = AlbumDetailFragment.newInstance(album.id, album.artworkUri)

            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, albumDetailFragment)
                .addToBackStack(null)
                .commit()
        }

        // 2. Alle Songs des Künstlers laden (Vertikal)
        val songs = MusicLoader.loadSongsForArtistName(requireContext().contentResolver, artistName)
        rvSongs.layoutManager = LinearLayoutManager(context)

        // Wir können direkt die Liste nutzen
        rvSongs.adapter = SongsAdapter(songs, showTrackNumber = true) { index ->
            // Der Adapter liefert uns jetzt direkt die Position (Int)
            // Wir rufen die playPlaylist der MainActivity mit der Liste und dem Index auf
            mainActivity?.playPlaylist(songs, index)
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