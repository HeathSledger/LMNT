package com.example.lmnt.ui.theme

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lmnt.MainActivity
import com.example.lmnt.R
import com.example.lmnt.adapter.ArtistAdapter
import com.example.lmnt.model.Artist
import com.example.lmnt.ui.ArtistDetailFragment

class ArtistsFragment : Fragment(R.layout.fragment_artists) {

    private lateinit var artistAdapter: ArtistAdapter

    // Listen für die Suche
    private var allArtists = listOf<Artist>()
    private val displayedArtists = mutableListOf<Artist>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.rvArtists) ?: return

        // 1. Daten über MainActivity laden
        allArtists = (activity as? MainActivity)?.loadArtists() ?: emptyList()

        // 2. Anzeige-Liste initial befüllen
        displayedArtists.clear()
        displayedArtists.addAll(allArtists)

        // 3. Adapter aufsetzen (arbeitet mit displayedArtists)
        artistAdapter = ArtistAdapter(displayedArtists) { artist ->
            openArtistDetails(artist)
        }

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = artistAdapter
    }

    private fun openArtistDetails(artist: Artist) {
        val detailFragment = ArtistDetailFragment.newInstance(artist.name)

        parentFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragment_container, detailFragment)
            .addToBackStack(null)
            .commit()
    }

    // DIE SUCHFUNKTION (Wird von der MainActivity aufgerufen)
    fun filter(query: String) {
        val lowerCaseQuery = query.lowercase()

        val filtered = if (lowerCaseQuery.isEmpty()) {
            allArtists
        } else {
            allArtists.filter { it.name.lowercase().contains(lowerCaseQuery) }
        }

        displayedArtists.clear()
        displayedArtists.addAll(filtered)

        // Sicherstellen, dass der Adapter schon existiert
        if (::artistAdapter.isInitialized) {
            artistAdapter.notifyDataSetChanged()
        }
    }
}