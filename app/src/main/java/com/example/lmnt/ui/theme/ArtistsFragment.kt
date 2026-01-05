package com.example.lmnt.ui

import android.content.Context
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

    // Wir nutzen nur noch diese eine Variable für den Status
    private var isListView = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Gespeicherten Modus aus SharedPreferences laden
        val prefs = requireContext().getSharedPreferences("LMNT_Settings", Context.MODE_PRIVATE)
        isListView = prefs.getBoolean("artists_is_list", true)

        // 2. RecyclerView initialisieren
        recyclerView = view.findViewById(R.id.rvArtists) ?: return

        // 3. LayoutManager basierend auf geladenem Wert setzen
        updateLayoutManager()

        // 4. Adapter initialisieren
        artistAdapter = ArtistAdapter(displayedArtists) { artist ->
            openArtistDetails(artist)
        }
        recyclerView.adapter = artistAdapter

        // 5. ViewModel anbinden
        musicViewModel = ViewModelProvider(requireActivity()).get(MusicViewModel::class.java)
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

    /**
     * Hilfsfunktion, um den LayoutManager zu setzen
     */
    private fun updateLayoutManager() {
        recyclerView.layoutManager = if (isListView) {
            LinearLayoutManager(context)
        } else {
            GridLayoutManager(context, 3)
        }
    }

    /**
     * Wechselt zwischen Liste und Grid und gibt den NEUEN Status zurück
     */
    fun toggleViewMode(): Boolean {
        isListView = !isListView
        updateLayoutManager()

        // Falls dein Adapter unterschiedliche Layouts für Liste/Grid nutzt,
        // musst du ihm hier bescheid geben.
        // Wenn er nur die Anordnung ändert, reicht notifyDataSetChanged()
        artistAdapter.notifyDataSetChanged()

        return isListView
    }

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