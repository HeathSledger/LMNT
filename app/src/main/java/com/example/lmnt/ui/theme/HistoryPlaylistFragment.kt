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

        // 1. UI-Elemente finden - HIER WAR DER FEHLER (R.id.btnCloseHistory statt R.drawable.x)
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvPlaylistSongs)
        val titleTv = view.findViewById<TextView>(R.id.tvPlaylistName)
        val btnClose = view.findViewById<ImageButton>(R.id.btnCloseHistory)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        titleTv.text = "Listening History"

        // 2. Schließen-Logik für das X
        btnClose?.setOnClickListener {
            val mainActivity = activity as? MainActivity

            // Fragment vom Stack nehmen
            parentFragmentManager.popBackStack()

            // Container in der MainActivity wieder auf GONE setzen
            val container = mainActivity?.findViewById<View>(R.id.fragment_container)
            container?.visibility = View.GONE

            // Titel in der Toolbar der MainActivity zurücksetzen
            val toolbarTitle = mainActivity?.findViewById<TextView>(R.id.toolbarTitle)
            toolbarTitle?.text = "LMNT"
        }

        // 3. Daten laden (Flow)
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
                        recyclerView.adapter = SongsAdapter(historySongs, showTrackNumber = false) { position ->
                            mainActivity?.playPlaylist(historySongs, position)
                        }
                    }
                }
            }
        }
    }
}