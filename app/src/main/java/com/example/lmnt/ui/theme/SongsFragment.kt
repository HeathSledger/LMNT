package com.example.lmnt.ui.theme

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lmnt.R
import com.example.lmnt.Song
import com.example.lmnt.SongsAdapter
import com.example.lmnt.MainActivity
import java.util.Locale

class SongsFragment : Fragment() {

    // Wir brauchen nur zwei Listen:
    private val allSongs = mutableListOf<Song>()      // Die komplette Mediathek
    private val displayedSongs = mutableListOf<Song>() // Das, was der User gerade sieht (gefiltert)
    private lateinit var adapter: SongsAdapter

    fun sortSongs(ascending: Boolean) {
        if (ascending) {
            allSongs.sortBy { it.title.lowercase() }
        } else {
            allSongs.sortByDescending { it.title.lowercase() }
        }

        // Wichtig: Auch die aktuell angezeigte (evtl. gefilterte) Liste aktualisieren
        updateDisplayedSongs(allSongs)
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

        // Der Adapter nutzt IMMER displayedSongs
        adapter = SongsAdapter(displayedSongs) { song ->
            playSong(song)
        }
        recyclerView.adapter = adapter
        recyclerView.isVerticalScrollBarEnabled = true

        checkPermissionsAndLoad()
    }

    private fun checkPermissionsAndLoad() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(requireContext(), permission)
            == PackageManager.PERMISSION_GRANTED) {
            loadSongs()
        } else {
            requestPermissions(arrayOf(permission), 101)
        }
    }

    private fun loadSongs() {
        // Hol dir die Daten einfach aus dem ViewModel der MainActivity
        val viewModel = androidx.lifecycle.ViewModelProvider(requireActivity()).get(com.example.lmnt.viewmodel.MusicViewModel::class.java)

        viewModel.songs.observe(viewLifecycleOwner) { songList ->
            allSongs.clear()
            allSongs.addAll(songList)
            updateDisplayedSongs(allSongs)
        }
    }

    // Diese Funktion aktualisiert die Anzeige und die Scrollbar-Buchstaben
    private fun updateDisplayedSongs(newList: List<Song>) {
        displayedSongs.clear()
        displayedSongs.addAll(newList)
        adapter.notifyDataSetChanged()
        adapter.setupSections() // Damit die Bubble die richtigen Buchstaben zeigt
    }

    // DIE SUCH-FUNKTION: Diese musst du von deiner MainActivity aus aufrufen!
    fun filter(query: String) {
        val lowerCaseQuery = query.lowercase(Locale.getDefault())
        if (lowerCaseQuery.isEmpty()) {
            updateDisplayedSongs(allSongs)
        } else {
            val filtered = allSongs.filter {
                it.title.lowercase().contains(lowerCaseQuery) ||
                        it.artist.lowercase().contains(lowerCaseQuery)
            }
            updateDisplayedSongs(filtered)
        }
    }

    private fun playSong(selectedSong: Song) {
        val startIndex = displayedSongs.indexOf(selectedSong)
        if (startIndex != -1) {
            // Wir übergeben die aktuell sichtbare Liste (damit nur Suchergebnisse im Player landen)
            (activity as? MainActivity)?.playPlaylist(displayedSongs, startIndex)
        }
    }
}