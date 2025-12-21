package com.example.lmnt.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface HistoryDao {
    @Insert
    suspend fun insert(entry: PlaybackHistory)

    @Query("SELECT * FROM playback_history ORDER BY timestamp DESC")
    suspend fun getAllHistory(): List<PlaybackHistory>

    @Query("SELECT songId, COUNT(songId) as playCount FROM playback_history GROUP BY songId ORDER BY playCount DESC LIMIT 10")
    suspend fun getAllTimeTopSongs(): List<SongPlayCount>

    // Speziell für deine Jahres-Statistik:
    @Query("SELECT songId, COUNT(songId) as playCount FROM playback_history WHERE year = :year GROUP BY songId ORDER BY playCount DESC LIMIT 10")
    suspend fun getTopSongsOfYear(year: Int): List<SongPlayCount>
}

// Hilfsklasse für die Top-Liste
data class SongPlayCount(
    val songId: Long,
    val playCount: Int
)