package com.example.lmnt.ui

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
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
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import java.util.Calendar
import com.example.lmnt.Song // Stelle sicher, dass dies auf dein Song-Modell zeigt
import kotlin.collections.find

class StatisticsFragment : Fragment(R.layout.fragment_statistics) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        // UI Elemente finden
        val tvTotalSongs = view.findViewById<TextView>(R.id.tvTotalSongs)
        val tvTotalDuration = view.findViewById<TextView>(R.id.tvTotalDuration)
        val rvRecentlyAdded = view.findViewById<RecyclerView>(R.id.rvRecentlyAdded)
        val rvTopSongs = view.findViewById<RecyclerView>(R.id.rvTopSongs)
        val chipYear = view.findViewById<Chip>(R.id.chipCurrentYear)

        // Dynamisches Jahr setzen
        chipYear.text = currentYear.toString()

        // 1. Basis-Musikdaten laden
        val allSongs = MusicLoader.loadAllSongs(requireContext().contentResolver)
        tvTotalSongs.text = "Total Number of Songs: ${allSongs.size}"

        val totalMs = allSongs.sumOf { it.duration.toLong() }
        val minutes = (totalMs / (1000 * 60)) % 60
        val hours = totalMs / (1000 * 60 * 60)
        tvTotalDuration.text = if (hours > 0) "Total Library Time: ${hours}h ${minutes}min" else "Total Library Time: ${minutes}min"

        // 2. New Songs
        val recentSongs = allSongs.takeLast(5).reversed()
        rvRecentlyAdded.layoutManager = LinearLayoutManager(context)
        rvRecentlyAdded.adapter = SongsAdapter(ArrayList(recentSongs)) { song ->
            (activity as? MainActivity)?.playPlaylist(recentSongs, recentSongs.indexOf(song))
        }

        // 3. Chart und Top Songs laden
        loadChart(view)
        loadYearlyStats(rvTopSongs, allSongs, currentYear)
    }

    private fun loadChart(view: View) {
        val chartContainer = view.findViewById<LinearLayout>(R.id.chartContainer)
        val tvMax = view.findViewById<TextView>(R.id.tvChartMax)
        val tvMid = view.findViewById<TextView>(R.id.tvChartMid)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        viewLifecycleOwner.lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val monthlyData = db.historyDao().getMonthlyPlays(currentYear)

            // Das Maximum in Millisekunden finden
            val maxMs = monthlyData.maxOfOrNull { it.count } ?: 0L

            // Umrechnung in Minuten für die Beschriftung
            val maxMinutes = maxMs / (1000 * 60)
            val midMinutes = maxMinutes / 2

            // Y-Achse Beschriftung mit "m" für Minuten
            if (maxMinutes > 0) {
                tvMax.text = "${maxMinutes}m"
                tvMid.text = "${midMinutes}m"
            } else {
                tvMax.text = "0m"
                tvMid.text = ""
            }

            val monthNames = arrayOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
            chartContainer.removeAllViews()

            for (i in 0..11) {
                val monthView = layoutInflater.inflate(R.layout.view_stat_bar, chartContainer, false)
                val barView = monthView.findViewById<View>(R.id.barView)
                val tvMonth = monthView.findViewById<TextView>(R.id.tvMonthName)

                tvMonth.text = monthNames[i]

                // Die Millisekunden für diesen Monat finden
                val durationMs = monthlyData.find { it.month == i + 1 }?.count ?: 0L

                val params = barView.layoutParams
                val density = resources.displayMetrics.density

                // Die Höhe wird relativ zum Millisekunden-Maximum berechnet
                val heightPx = if (durationMs > 0 && maxMs > 0) {
                    ((durationMs.toFloat() / maxMs) * 160 * density).toInt()
                } else {
                    (4 * density).toInt() // Minimaler Punkt
                }

                params.height = heightPx
                barView.layoutParams = params
                chartContainer.addView(monthView)
            }
        }
    }

    private fun loadYearlyStats(recyclerView: RecyclerView?, allSongs: List<Song>, year: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val topEntries = db.historyDao().getTopSongsOfYear(year)

            val topSongsList = topEntries.mapNotNull { entry ->
                allSongs.find { it.id == entry.songId }
            }

            recyclerView?.layoutManager = LinearLayoutManager(context)
            recyclerView?.adapter = SongsAdapter(ArrayList(topSongsList)) { song ->
                (activity as? MainActivity)?.playPlaylist(topSongsList, topSongsList.indexOf(song))
            }
        }
    }
}