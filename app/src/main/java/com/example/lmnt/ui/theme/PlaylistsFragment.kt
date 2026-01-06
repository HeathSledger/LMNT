package com.example.lmnt.ui.theme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lmnt.MainActivity
import com.example.lmnt.R
import com.example.lmnt.Song // Stelle sicher, dass dies auf deine Song-Klasse zeigt
import com.example.lmnt.viewmodel.MusicViewModel

class PlaylistsFragment : Fragment() {

    private lateinit var musicViewModel: MusicViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PlaylistAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_playlists, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewPlaylists)
        recyclerView.layoutManager = LinearLayoutManager(context)

        musicViewModel = ViewModelProvider(requireActivity()).get(MusicViewModel::class.java)

        // Beobachte die Favoriten
        musicViewModel.favoriteSongs.observe(viewLifecycleOwner) { favSongs ->
            // FEHLERBEHEBUNG: favSongs ist eine List<Song>, nicht List<SongsFragment>
            updateList(favSongs)
        }

        musicViewModel.refreshFavorites()

        return view
    }

    // KORREKTUR: Der Parameter-Typ muss List<Song> sein
    private fun updateList(favSongs: List<Song>) {
        val items = mutableListOf<PlaylistItem>()

        // Favoriten-Eintrag erstellen
        items.add(PlaylistItem(
            id = -1,
            name = "Favoriten",
            songCount = favSongs.size,
            isFavoriteType = true
        ))

        // Adapter setzen
        adapter = PlaylistAdapter(items) { item ->
            if (item.isFavoriteType) {
                // Hier rufen wir die neue Instanz des SongsFragments auf
                (activity as? MainActivity)?.showFragment(SongsFragment.newInstance(onlyFavorites = true))
            }
        }
        recyclerView.adapter = adapter
    }
}

// Hilfsklasse für die Liste (unverändert)
data class PlaylistItem(
    val id: Long,
    val name: String,
    val songCount: Int,
    val isFavoriteType: Boolean = false
)