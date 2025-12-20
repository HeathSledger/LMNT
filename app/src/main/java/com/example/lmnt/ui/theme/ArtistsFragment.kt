package com.example.lmnt.ui.theme

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lmnt.MainActivity
import com.example.lmnt.R
import com.example.lmnt.adapter.ArtistAdapter // Den müsstest du analog zum AlbumAdapter erstellen
import com.example.lmnt.model.Artist
import com.example.lmnt.ui.ArtistDetailFragment // Falls es in einem anderen Unterordner liegt


class ArtistsFragment : Fragment(R.layout.fragment_artists) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Wir suchen die RecyclerView im Layout des Fragments
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvArtists) ?: return

        val artists = (activity as? MainActivity)?.loadArtists() ?: emptyList()

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = ArtistAdapter(artists) { artist ->
            openArtistDetails(artist)
        }
    }

    private fun openArtistDetails(artist: Artist) {
        // Hier nutzen wir artist.name für die Anzeige
        val detailFragment = ArtistDetailFragment.newInstance(artist.name)

        parentFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            // R.id.fragment_container ist in activity_main.xml
            .replace(R.id.fragment_container, detailFragment)
            .addToBackStack(null)
            .commit()
    }
}