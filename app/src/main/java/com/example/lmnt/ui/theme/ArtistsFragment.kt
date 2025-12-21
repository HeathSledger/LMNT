package com.example.lmnt.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lmnt.R
import com.example.lmnt.adapter.ArtistAdapter
import com.example.lmnt.model.Artist
import com.example.lmnt.viewmodel.MusicViewModel

class ArtistsFragment : Fragment(R.layout.fragment_artists) {

    private lateinit var artistAdapter: ArtistAdapter
    private lateinit var musicViewModel: MusicViewModel
    private lateinit var recyclerView: RecyclerView

    private val displayedArtists = mutableListOf<Artist>()
    private var isGridView = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.rvArtists) ?: return

        artistAdapter = ArtistAdapter(displayedArtists) { artist ->
            openArtistDetails(artist)
        }
        recyclerView.adapter = artistAdapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        musicViewModel = ViewModelProvider(requireActivity()).get(MusicViewModel::class.java)

        // Zentraler Datenempfang vom ViewModel
        musicViewModel.artists.observe(viewLifecycleOwner) { artistList ->
            updateDisplayedArtists(artistList)
        }
    }

    private fun updateDisplayedArtists(newList: List<Artist>) {
        displayedArtists.clear()
        displayedArtists.addAll(newList)
        if (::artistAdapter.isInitialized) {
            artistAdapter.notifyDataSetChanged()
        }
    }

    // Die Funktion toggleViewMode ist super so!
    fun toggleViewMode() {
        isGridView = !isGridView
        recyclerView.layoutManager = if (isGridView) {
            GridLayoutManager(context, 3)
        } else {
            LinearLayoutManager(context)
        }
        // Wichtig bei LayoutManager-Wechsel:
        recyclerView.adapter?.notifyDataSetChanged()
    }

    // filter() wurde entfernt, da das ViewModel das übernimmt.

    private fun openArtistDetails(artist: Artist) {
        activity?.findViewById<View>(R.id.fragment_container)?.visibility = View.VISIBLE
        val detailFragment = ArtistDetailFragment.newInstance(artist.name)

        parentFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragment_container, detailFragment)
            .addToBackStack(null)
            .commit()
    }
}