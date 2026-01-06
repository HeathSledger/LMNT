package com.example.lmnt.ui.theme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lmnt.R
import com.example.lmnt.MainActivity
import com.example.lmnt.Song        // KORREKTUR: Import hinzugefügt
import com.example.lmnt.SongsAdapter
import com.example.lmnt.viewmodel.MusicViewModel

class SongsFragment : Fragment() {

    private val displayedSongs = mutableListOf<Song>()
    private lateinit var adapter: SongsAdapter
    private lateinit var musicViewModel: MusicViewModel
    private var isOnlyFavorites = false

    companion object {
        fun newInstance(onlyFavorites: Boolean = false): SongsFragment {
            val fragment = SongsFragment()
            val args = Bundle()
            args.putBoolean("onlyFavorites", onlyFavorites)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isOnlyFavorites = arguments?.getBoolean("onlyFavorites") ?: false
    }

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

        musicViewModel = ViewModelProvider(requireActivity()).get(MusicViewModel::class.java)

        // KORREKTUR: Parameter 'onClick' statt 'onItemClick' passend zum Adapter
        adapter = SongsAdapter(
            songs = displayedSongs,
            onClick = { index -> playSong(index) },
            onLongClick = { song -> (activity as? MainActivity)?.showSongOptions(song) }
        )
        recyclerView.adapter = adapter

        musicViewModel.songs.observe(viewLifecycleOwner) { songList ->
            lifecycleScope.launchWhenStarted {
                val filteredList = if (isOnlyFavorites) {
                    val favIds = musicViewModel.getFavoriteIds()
                    songList.filter { it.id in favIds }
                } else {
                    songList
                }

                displayedSongs.clear()
                displayedSongs.addAll(filteredList)
                adapter.notifyDataSetChanged()
                adapter.setupSections() // Aktualisiert das Alphabet-Verzeichnis
            }
        }
    }

    private fun playSong(startIndex: Int) {
        if (startIndex in displayedSongs.indices) {
            (activity as? MainActivity)?.playPlaylist(ArrayList(displayedSongs), startIndex)
        }
    }
}