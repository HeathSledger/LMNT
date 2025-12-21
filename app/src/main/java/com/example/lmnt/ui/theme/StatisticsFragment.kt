package com.example.lmnt.ui

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
import kotlinx.coroutines.launch
import java.util.Calendar

class StatisticsFragment : Fragment(R.layout.fragment_statistics) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        // Den Chip dynamisch auf das aktuelle Jahr setzen
        val chipYear = view.findViewById<com.google.android.material.chip.Chip>(R.id.chip2025)
        chipYear.text = currentYear.toString()

        val tvTotalSongs = view.findViewById<TextView>(R.id.tvTotalSongs)
        val tvTotalDuration = view.findViewById<TextView>(R.id.tvTotalDuration)
        val rvRecentlyAdded = view.findViewById<RecyclerView>(R.id.rvRecentlyAdded)

        // 1. Basis-Daten laden (Alle Songs für die Berechnung)
        val allSongs = MusicLoader.loadAllSongs(requireContext().contentResolver)

        // Songs insgesamt
        tvTotalSongs.text = "Total Number of Songs: ${allSongs.size}"

        // Gesamtdauer berechnen
        val totalMs = allSongs.sumOf { it.duration.toLong() }
        val totalMinutes = totalMs / 1000 / 60
        val hours = totalMinutes / 60
        val remainingMinutes = totalMinutes % 60
        tvTotalDuration.text = if (hours > 0) "Total Time: ${hours}h ${remainingMinutes}min" else "Total Time: ${totalMinutes}min"

        // 2. "New Songs" (Letzte 5 aus dem Speicher)
        val recentSongs = allSongs.takeLast(5).reversed()
        rvRecentlyAdded.layoutManager = LinearLayoutManager(context)
        rvRecentlyAdded.adapter = SongsAdapter(ArrayList(recentSongs)) { song ->
            (activity as? MainActivity)?.playPlaylist(recentSongs, recentSongs.indexOf(song))
        }

        // 3. ECHTE STATISTIKEN (Spotify-Style) laden
        loadYearlyTopSongs(view, allSongs)
    }

    private fun loadFilteredStats(recyclerView: RecyclerView?, allSongs: List<com.example.lmnt.model.Song>, filterType: String) {
        val calendar = Calendar.getInstance()

        viewLifecycleOwner.lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())

            val topEntries = when(filterType) {
                "YEAR" -> db.historyDao().getTopSongsOfYear(calendar.get(Calendar.YEAR))
                "MONTH" -> {
                    // Hierfür müssten wir eine kleine Query im DAO ergänzen
                    // oder wir filtern die All-Time Liste lokal
                    db.historyDao().getTopSongsOfYear(calendar.get(Calendar.YEAR)) // Platzhalter
                }
                else -> db.historyDao().getAllTimeTopSongs() // Du müsstest diese Query noch im DAO einfügen
            }

            val topSongsList = topEntries.mapNotNull { entry ->
                allSongs.find { it.id == entry.songId }
            }

            recyclerView?.adapter = SongsAdapter(ArrayList(topSongsList)) { song ->
                (activity as? MainActivity)?.playPlaylist(topSongsList, topSongsList.indexOf(song))
            }
        }
    }
}