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

    @Query("SELECT DISTINCT year FROM playback_history ORDER BY year DESC")
    suspend fun getAvailableYears(): List<Int>

    // --- TOP SONGS (Sortiert nach Zeit) ---
    @Query("SELECT songId, SUM(duration) as totalTime FROM playback_history WHERE year = :year GROUP BY songId ORDER BY totalTime DESC LIMIT 10")
    suspend fun getTopSongsOfYear(year: Int): List<SongTimeStat>

    @Query("SELECT songId, SUM(duration) as totalTime FROM playback_history GROUP BY songId ORDER BY totalTime DESC LIMIT 10")
    suspend fun getAllTimeTopSongs(): List<SongTimeStat>

    // --- TOP ARTISTS (Sortiert nach Zeit) ---
    @Query("SELECT artist, SUM(duration) as totalTime FROM playback_history WHERE year = :year GROUP BY artist ORDER BY totalTime DESC LIMIT 5")
    suspend fun getTop5Artists(year: Int): List<ArtistTimeStat>

    @Query("SELECT artist, SUM(duration) as totalTime FROM playback_history GROUP BY artist ORDER BY totalTime DESC LIMIT 5")
    suspend fun getAllTimeTopArtists(): List<ArtistTimeStat>

    // --- TOP ALBUMS (NEU!) ---
    @Query("SELECT album, artist, SUM(duration) as totalTime FROM playback_history WHERE year = :year GROUP BY album ORDER BY totalTime DESC LIMIT 5")
    suspend fun getTop5Albums(year: Int): List<AlbumTimeStat>

    @Query("SELECT album, artist, SUM(duration) as totalTime FROM playback_history GROUP BY album ORDER BY totalTime DESC LIMIT 5")
    suspend fun getAllTimeTopAlbums(): List<AlbumTimeStat>

    // --- CHART & GESAMTZEIT ---
    @Query("SELECT month, SUM(duration) as count FROM playback_history WHERE year = :year GROUP BY month ORDER BY month ASC")
    suspend fun getMonthlyPlays(year: Int): List<MonthlyCount>

    @Query("SELECT SUM(duration) FROM playback_history WHERE year = :year")
    suspend fun getTotalPlaytimeMs(year: Int): Long?

    @Query("SELECT SUM(duration) FROM playback_history")
    suspend fun getAllTimeTotalPlaytimeMs(): Long?
}

// --- Hilfsklassen für zeitbasierte Ergebnisse ---

data class SongTimeStat(
    val songId: Long,
    val totalTime: Long
)

data class ArtistTimeStat(
    val artist: String,
    val totalTime: Long
)

data class AlbumTimeStat(
    val album: String,
    val artist: String,
    val totalTime: Long
)

data class MonthlyCount(
    val month: Int,
    val count: Long
)