package com.example.lmnt.ui

import android.graphics.Color
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
import com.example.lmnt.Song
import com.example.lmnt.model.Album
import com.example.lmnt.database.*
import com.example.lmnt.database.TopArtistAdapter
import com.example.lmnt.database.TopAlbumAdapter
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class StatisticsFragment : Fragment(R.layout.fragment_statistics) {

    private lateinit var allSongs: List<Song>
    private lateinit var allAlbums: List<Album>
    private lateinit var tvYearlyTimeValue: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvRecentlyAdded = view.findViewById<RecyclerView>(R.id.rvRecentlyAdded)
        val rvTopSongs = view.findViewById<RecyclerView>(R.id.rvTopSongs)
        val rvTopArtists = view.findViewById<RecyclerView>(R.id.rvTopArtists)
        val rvTopAlbums = view.findViewById<RecyclerView>(R.id.rvTopAlbums)
        val tvTotalSongs = view.findViewById<TextView>(R.id.tvTotalSongs)
        val tvTotalDuration = view.findViewById<TextView>(R.id.tvTotalDuration)
        tvYearlyTimeValue = view.findViewById(R.id.tvYearlyTimeValue)

        viewLifecycleOwner.lifecycleScope.launch {
            val ctx = context ?: return@launch

            withContext(Dispatchers.IO) {
                allSongs = MusicLoader.loadAllSongs(ctx.contentResolver)
                allAlbums = MusicLoader.loadAlbums(ctx.contentResolver)
            }

            tvTotalSongs.text = "Total Number of Songs: ${allSongs.size}"
            val totalMs = allSongs.sumOf { it.duration.toLong() }
            tvTotalDuration.text = "Total Library Time: ${totalMs / (1000 * 60)} Min"

            // 3. Recently Added - KORRIGIERT
            val recentSongs = allSongs.takeLast(5).reversed()
            rvRecentlyAdded.layoutManager = LinearLayoutManager(ctx)
            rvRecentlyAdded.adapter = SongsAdapter(recentSongs) { index ->
                // Wir nutzen direkt den Index vom Adapter
                (activity as? MainActivity)?.playPlaylist(recentSongs, index)
            }

            setupYearFilters(view, rvTopSongs, rvTopArtists, rvTopAlbums)
        }
    }

    private fun setupYearFilters(view: View, rvSongs: RecyclerView, rvArtists: RecyclerView, rvAlbums: RecyclerView) {
        val chipGroup = view.findViewById<ChipGroup>(R.id.chipGroupTime)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        viewLifecycleOwner.lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val yearsFromDb = db.historyDao().getAvailableYears()

            chipGroup.removeAllViews()

            addYearChip(chipGroup, "All Time", null, rvSongs, rvArtists, rvAlbums)

            if (yearsFromDb.isEmpty()) {
                addYearChip(chipGroup, currentYear.toString(), currentYear, rvSongs, rvArtists, rvAlbums, true)
            } else {
                yearsFromDb.forEach { year ->
                    addYearChip(chipGroup, year.toString(), year, rvSongs, rvArtists, rvAlbums, year == currentYear)
                }
            }
        }
    }

    private fun addYearChip(group: ChipGroup, label: String, year: Int?, rvS: RecyclerView, rvAr: RecyclerView, rvAl: RecyclerView, isSelected: Boolean = false) {
        val chip = Chip(requireContext()).apply {
            text = label
            isCheckable = true
            isChecked = isSelected
            setTextColor(Color.WHITE)
        }

        chip.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                loadTimeBasedStats(year, rvS, rvAr, rvAl)
                if (year != null) updateChart(year)
            }
        }
        group.addView(chip)
        if (isSelected) {
            loadTimeBasedStats(year, rvS, rvAr, rvAl)
            if (year != null) updateChart(year)
        }
    }

    private fun loadTimeBasedStats(year: Int?, rvSongs: RecyclerView, rvArtists: RecyclerView, rvAlbums: RecyclerView) {
        viewLifecycleOwner.lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())

            val totalYearlyMs = if (year != null) db.historyDao().getTotalPlaytimeMs(year) else db.historyDao().getAllTimeTotalPlaytimeMs()
            val totalMinutes = (totalYearlyMs ?: 0L) / (1000 * 60)
            tvYearlyTimeValue.text = "$totalMinutes Minutes"

            // 2. Top Songs - KORRIGIERT
            val topEntries = if (year != null) db.historyDao().getTopSongsOfYear(year) else db.historyDao().getAllTimeTopSongs()
            val topSongsList = topEntries.mapNotNull { entry -> allSongs.find { it.id == entry.songId } }

            rvSongs.layoutManager = LinearLayoutManager(context)
            rvSongs.adapter = SongsAdapter(topSongsList) { index ->
                // Wir nutzen direkt den Index vom Adapter
                (activity as? MainActivity)?.playPlaylist(topSongsList, index)
            }

            val topArtistsData = if (year != null) db.historyDao().getTop5Artists(year) else db.historyDao().getAllTimeTopArtists()
            rvArtists.layoutManager = LinearLayoutManager(context)
            rvArtists.adapter = TopArtistAdapter(topArtistsData, allAlbums)

            val topAlbumsData = if (year != null) db.historyDao().getTop5Albums(year) else db.historyDao().getAllTimeTopAlbums()
            rvAlbums.layoutManager = LinearLayoutManager(context)
            rvAlbums.adapter = TopAlbumAdapter(topAlbumsData, allAlbums)
        }
    }

    private fun updateChart(year: Int) {
        val chartContainer = view?.findViewById<LinearLayout>(R.id.chartContainer) ?: return
        val tvMax = view?.findViewById<TextView>(R.id.tvChartMax) ?: return
        val tvMid = view?.findViewById<TextView>(R.id.tvChartMid) ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val monthlyData = db.historyDao().getMonthlyPlays(year)

            val maxMs = monthlyData.maxOfOrNull { it.count } ?: 0L
            val maxMinutes = maxMs / (1000 * 60)
            tvMax.text = "${maxMinutes}m"
            tvMid.text = "${maxMinutes / 2}m"

            val monthNames = arrayOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
            chartContainer.removeAllViews()

            for (i in 0..11) {
                val monthView = layoutInflater.inflate(R.layout.view_stat_bar, chartContainer, false)
                val barView = monthView.findViewById<View>(R.id.barView)
                (monthView.findViewById<TextView>(R.id.tvMonthName)).text = monthNames[i]

                val durationMs = monthlyData.find { it.month == i + 1 }?.count ?: 0L
                val params = barView.layoutParams
                val density = resources.displayMetrics.density
                params.height = if (maxMs > 0) ((durationMs.toFloat() / maxMs) * 160 * density).toInt().coerceAtLeast((4 * density).toInt()) else (4 * density).toInt()
                barView.layoutParams = params
                chartContainer.addView(monthView)
            }
        }
    }
}