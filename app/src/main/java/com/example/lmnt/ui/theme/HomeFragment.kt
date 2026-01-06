package com.example.lmnt.ui.theme

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.lmnt.MainActivity
import com.example.lmnt.MusicLoader
import com.example.lmnt.R
import com.example.lmnt.Song
import com.example.lmnt.adapter.RecentlyAddedAdapter
import com.example.lmnt.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment(R.layout.fragment_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = AppDatabase.getDatabase(requireContext())
        val allSongs = MusicLoader.loadAllSongs(requireContext().contentResolver)

        // 1. Begrüßung & Shuffle
        view.findViewById<TextView>(R.id.tvGreeting).text = getGreeting()
        view.findViewById<View>(R.id.btnRandomPlay).setOnClickListener {
            if (allSongs.isNotEmpty()) {
                // KORREKTUR: List zu ArrayList konvertieren
                val shuffledList = ArrayList(allSongs.shuffled())
                (activity as? MainActivity)?.playPlaylist(shuffledList, 0)
            }
        }

        // 2. See All History
        val tvSeeAll = view.findViewById<TextView>(R.id.tvSeeAllHistory)
        tvSeeAll.setOnClickListener {
            (activity as? MainActivity)?.showFragment(HistoryPlaylistFragment())
        }

        val rvRecentlyPlayed = view.findViewById<RecyclerView>(R.id.rvRecentlyPlayed)
        val rvRecentlyAdded = view.findViewById<RecyclerView>(R.id.rvRecentlyAdded)
        val rvRediscover = view.findViewById<RecyclerView>(R.id.rvRediscover)

        // 3. Recently Added
        val recentAddedSongs = MusicLoader.loadRecentlyAdded(requireContext().contentResolver, limit = 15)
        rvRecentlyAdded.adapter = RecentlyAddedAdapter(recentAddedSongs) { song ->
            val index = recentAddedSongs.indexOf(song)
            // KORREKTUR: ArrayList explizit erstellen
            (activity as? MainActivity)?.playPlaylist(ArrayList(recentAddedSongs), index)
        }

        // 4. LIVE: Recently Played
        lifecycleScope.launch(Dispatchers.IO) {
            db.historyDao().getRecentPlayedIdsFlow(15).collect { historyIds ->
                val recentPlayedSongs = historyIds.mapNotNull { id ->
                    allSongs.find { it.id == id }
                }

                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    rvRecentlyPlayed.adapter = RecentlyAddedAdapter(recentPlayedSongs) { song ->
                        val index = recentPlayedSongs.indexOf(song)
                        // KORREKTUR: ArrayList explizit erstellen
                        (activity as? MainActivity)?.playPlaylist(ArrayList(recentPlayedSongs), index)
                    }
                    tvSeeAll.visibility = if (recentPlayedSongs.isNotEmpty()) View.VISIBLE else View.GONE
                }
            }
        }

        // 5. Forgotten Gems (Rediscover)
        val twoWeeksAgo = System.currentTimeMillis() - (14 * 24 * 60 * 60 * 1000L)
        lifecycleScope.launch(Dispatchers.IO) {
            val metaList = db.historyDao().getRediscoverMetadata(twoWeeksAgo)
            val rediscoverIds = metaList.map { it.songId }.toSet()
            val rediscoverSongs = allSongs.filter { it.id in rediscoverIds }.shuffled().take(15)

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext
                rvRediscover.adapter = RecentlyAddedAdapter(rediscoverSongs) { song ->
                    val index = rediscoverSongs.indexOf(song)
                    // KORREKTUR: ArrayList explizit erstellen
                    (activity as? MainActivity)?.playPlaylist(ArrayList(rediscoverSongs), index)
                }
            }
        }
    }

    private fun getGreeting(): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Good Morning"
            in 12..17 -> "Good Afternoon"
            in 18..22 -> "Good Evening"
            else -> "Good Night"
        }
    }
}