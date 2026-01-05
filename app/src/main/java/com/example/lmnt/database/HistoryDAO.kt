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

    // --- TOP SONGS ---
    @Query("SELECT songId, SUM(duration) as totalTime FROM playback_history WHERE year = :year GROUP BY songId ORDER BY totalTime DESC LIMIT 10")
    suspend fun getTopSongsOfYear(year: Int): List<SongTimeStat>

    @Query("SELECT songId, SUM(duration) as totalTime FROM playback_history GROUP BY songId ORDER BY totalTime DESC LIMIT 10")
    suspend fun getAllTimeTopSongs(): List<SongTimeStat>

    // --- TOP ARTISTS ---
    // Wir nehmen die songId eines Songs dieses Künstlers mit, um ein Vorschaubild zu haben
    @Query("SELECT artist, SUM(duration) as totalTime, MAX(songId) as representativeSongId FROM playback_history WHERE year = :year GROUP BY artist ORDER BY totalTime DESC LIMIT 5")
    suspend fun getTop5Artists(year: Int): List<ArtistTimeStat>

    @Query("SELECT artist, SUM(duration) as totalTime, MAX(songId) as representativeSongId FROM playback_history GROUP BY artist ORDER BY totalTime DESC LIMIT 5")
    suspend fun getAllTimeTopArtists(): List<ArtistTimeStat>

    // --- TOP ALBUMS ---
    // Wir speichern die songId mit, um später das Album-Cover über den ContentResolver zu finden
    @Query("SELECT album, artist, SUM(duration) as totalTime, MAX(songId) as representativeSongId FROM playback_history WHERE year = :year GROUP BY album ORDER BY totalTime DESC LIMIT 5")
    suspend fun getTop5Albums(year: Int): List<AlbumTimeStat>

    @Query("SELECT album, artist, SUM(duration) as totalTime, MAX(songId) as representativeSongId FROM playback_history GROUP BY album ORDER BY totalTime DESC LIMIT 5")
    suspend fun getAllTimeTopAlbums(): List<AlbumTimeStat>

    // --- STATS ---
    @Query("SELECT month, SUM(duration) as count FROM playback_history WHERE year = :year GROUP BY month ORDER BY month ASC")
    suspend fun getMonthlyPlays(year: Int): List<MonthlyCount>

    @Query("SELECT SUM(duration) FROM playback_history WHERE year = :year")
    suspend fun getTotalPlaytimeMs(year: Int): Long?

    @Query("SELECT SUM(duration) FROM playback_history")
    suspend fun getAllTimeTotalPlaytimeMs(): Long?

    @Insert(onConflict = androidx.room.OnConflictStrategy.IGNORE)
    suspend fun insertInitialMetadata(metadata: SongMetadata)

    @Query("UPDATE song_metadata SET lastPlayed = :timestamp, playCount = playCount + 1 WHERE songId = :id")
    suspend fun updateLastPlayed(id: Long, timestamp: Long)

    @Query("SELECT * FROM song_metadata WHERE lastPlayed < :threshold OR lastPlayed = 0")
    suspend fun getRediscoverMetadata(threshold: Long): List<SongMetadata>

    @Query("UPDATE song_metadata SET isFavorite = :isFavorite WHERE songId = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("""
    SELECT songId FROM playback_history 
    GROUP BY songId 
    ORDER BY MAX(timestamp) DESC 
    LIMIT :limit
""")
    fun getRecentPlayedIdsFlow(limit: Int): kotlinx.coroutines.flow.Flow<List<Long>>
}

// --- Aktualisierte Hilfsklassen ---

data class SongTimeStat(
    val songId: Long,
    val totalTime: Long
)

data class ArtistTimeStat(
    val artist: String,
    val totalTime: Long,
    val representativeSongId: Long // NEU: Für das Bild
)

data class AlbumTimeStat(
    val album: String,
    val artist: String,
    val totalTime: Long,
    val representativeSongId: Long // NEU: Für das Bild
)

data class MonthlyCount(
    val month: Int,
    val count: Long
)