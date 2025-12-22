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

        // ÄNDERUNG: Der Adapter liefert jetzt den Index (Int) statt des Songs
        adapter = SongsAdapter(displayedSongs) { index ->
            playSong(index)
        }
        recyclerView.adapter = adapter

        musicViewModel = ViewModelProvider(requireActivity()).get(MusicViewModel::class.java)

        musicViewModel.songs.observe(viewLifecycleOwner) { songList ->
            displayedSongs.clear()
            displayedSongs.addAll(songList)
            adapter.notifyDataSetChanged()
            adapter.setupSections()
        }
    }

    // ÄNDERUNG: Diese Funktion nimmt jetzt direkt den Index entgegen
    private fun playSong(startIndex: Int) {
        if (startIndex in displayedSongs.indices) {
            // Die gesamte Liste und den Startpunkt an die MainActivity übergeben
            (activity as? MainActivity)?.playPlaylist(ArrayList(displayedSongs), startIndex)
        }
    }
}