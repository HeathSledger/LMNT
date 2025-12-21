package com.example.lmnt.ui // Geändert von .ui.theme zu .ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lmnt.MainActivity
import com.example.lmnt.R
import com.example.lmnt.adapter.ArtistAdapter
import com.example.lmnt.model.Artist

class ArtistsFragment : Fragment(R.layout.fragment_artists) {

    private lateinit var artistAdapter: ArtistAdapter
    private var allArtists = listOf<Artist>()
    private val displayedArtists = mutableListOf<Artist>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.rvArtists) ?: return

        // Daten über MainActivity laden
        allArtists = (activity as? MainActivity)?.loadArtists() ?: emptyList()

        displayedArtists.clear()
        displayedArtists.addAll(allArtists)

        artistAdapter = ArtistAdapter(displayedArtists) { artist ->
            openArtistDetails(artist)
        }

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = artistAdapter
    }

    private fun openArtistDetails(artist: Artist) {
        // WICHTIG: Den Container sichtbar machen, damit man das Detail sieht
        activity?.findViewById<View>(R.id.fragment_container)?.visibility = View.VISIBLE

        val detailFragment = ArtistDetailFragment.newInstance(artist.name)

        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in, android.R.anim.fade_out,
                android.R.anim.fade_in, android.R.anim.fade_out
            )
            .replace(R.id.fragment_container, detailFragment)
            .addToBackStack(null)
            .commit()
    }

    // Die Suchfunktion bleibt so, sie ist korrekt!
    fun filter(query: String) {
        val lowerCaseQuery = query.lowercase()

        val filtered = if (lowerCaseQuery.isEmpty()) {
            allArtists
        } else {
            allArtists.filter { it.name.lowercase().contains(lowerCaseQuery) }
        }

        displayedArtists.clear()
        displayedArtists.addAll(filtered)

        if (::artistAdapter.isInitialized) {
            artistAdapter.notifyDataSetChanged()
        }
    }
}