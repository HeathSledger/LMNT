package com.example.lmnt.ui.theme

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lmnt.MainActivity
import com.example.lmnt.MusicLoader
import com.example.lmnt.R
import com.example.lmnt.SongsAdapter
import com.example.lmnt.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryPlaylistFragment : Fragment(R.layout.fragment_playlist_detail) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.rvPlaylistSongs)
        val titleTv = view.findViewById<TextView>(R.id.tvPlaylistName)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        titleTv.text = "Listening History"

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val allSongs = MusicLoader.loadAllSongs(requireContext().contentResolver)

            // Wir nutzen hier den Flow (getRecentPlayedIdsFlow)
            db.historyDao().getRecentPlayedIdsFlow(100).collect { historyIds ->

                // Mapping im Hintergrund-Thread für bessere Performance bei 100 Songs
                val historySongs = withContext(Dispatchers.Default) {
                    historyIds.mapNotNull { id ->
                        allSongs.find { it.id == id }
                    }
                }

                withContext(Dispatchers.Main) {
                    if (historySongs.isEmpty()) {
                        titleTv.text = "No history yet"
                    } else {
                        val mainActivity = activity as? MainActivity
                        recyclerView.adapter = SongsAdapter(historySongs, showTrackNumber = false) { position ->
                            mainActivity?.playPlaylist(historySongs, position)
                        }
                    }
                }
            }
        }
    }
}