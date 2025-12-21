package com.example.lmnt.ui.theme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lmnt.R
import com.example.lmnt.Song
import com.example.lmnt.SongsAdapter
import com.example.lmnt.MainActivity
import com.example.lmnt.viewmodel.MusicViewModel

class SongsFragment : Fragment() {

    private val displayedSongs = mutableListOf<Song>()
    private lateinit var adapter: SongsAdapter
    private lateinit var musicViewModel: MusicViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_songs, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.songsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = SongsAdapter(displayedSongs) { song ->
            playSong(song)
        }
        recyclerView.adapter = adapter

        // ViewModel von der Activity holen (Wichtig: requireActivity())
        musicViewModel = ViewModelProvider(requireActivity()).get(MusicViewModel::class.java)

        // Hier passiert die Magie: Das ViewModel sagt uns, was wir anzeigen sollen.
        // Egal ob gefiltert, sortiert oder neu geladen.
        musicViewModel.songs.observe(viewLifecycleOwner) { songList ->
            displayedSongs.clear()
            displayedSongs.addAll(songList)
            adapter.notifyDataSetChanged()
            adapter.setupSections() // Wichtig für deine A-Z Sidebar
        }
    }
    private fun playSong(selectedSong: Song) {
        val startIndex = displayedSongs.indexOf(selectedSong)
        if (startIndex != -1) {
            // Kopie der aktuellen Liste an den Player übergeben
            (activity as? MainActivity)?.playPlaylist(ArrayList(displayedSongs), startIndex)
        }
    }
}