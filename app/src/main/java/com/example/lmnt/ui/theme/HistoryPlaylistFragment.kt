package com.example.lmnt.ui.theme

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryPlaylistFragment : Fragment(R.layout.fragment_playlist_detail) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.rvPlaylistSongs)
        val titleTv = view.findViewById<TextView>(R.id.tvPlaylistName)
        val btnClose = view.findViewById<ImageButton>(R.id.btnCloseHistory)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        titleTv.text = "Listening History"

        btnClose?.setOnClickListener {
            val mainActivity = activity as? MainActivity
            parentFragmentManager.popBackStack()
            val container = mainActivity?.findViewById<View>(R.id.fragment_container)
            container?.visibility = View.GONE
            val toolbarTitle = mainActivity?.findViewById<TextView>(R.id.toolbarTitle)
            toolbarTitle?.text = "LMNT"
        }

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val allSongs = MusicLoader.loadAllSongs(requireContext().contentResolver)

            db.historyDao().getRecentPlayedIdsFlow(100).collect { historyIds ->
                val historySongs = withContext(Dispatchers.Default) {
                    historyIds.mapNotNull { id ->
                        allSongs.find { it.id == id }
                    }
                }

                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext

                    if (historySongs.isEmpty()) {
                        titleTv.text = "No history yet"
                    } else {
                        val mainActivity = activity as? MainActivity

                        // KORREKTUR: Benannte Parameter nutzen
                        recyclerView.adapter = SongsAdapter(
                            songs = historySongs,
                            showTrackNumber = false,
                            onClick = { position ->
                                // Umwandlung in ArrayList für die MainActivity
                                mainActivity?.playPlaylist(ArrayList(historySongs), position)
                            },
                            onLongClick = { song ->
                                // Ermöglicht Favorisieren direkt aus der History
                                mainActivity?.showSongOptions(song)
                            }
                        )
                    }
                }
            }
        }
    }
}