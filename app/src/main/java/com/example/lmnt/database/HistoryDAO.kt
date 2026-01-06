package com.example.lmnt.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    // --- PLAYBACK HISTORY ---
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
    @Query("SELECT artist, SUM(duration) as totalTime, MAX(songId) as representativeSongId FROM playback_history WHERE year = :year GROUP BY artist ORDER BY totalTime DESC LIMIT 5")
    suspend fun getTop5Artists(year: Int): List<ArtistTimeStat>

    @Query("SELECT artist, SUM(duration) as totalTime, MAX(songId) as representativeSongId FROM playback_history GROUP BY artist ORDER BY totalTime DESC LIMIT 5")
    suspend fun getAllTimeTopArtists(): List<ArtistTimeStat>

    // --- TOP ALBUMS ---
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

    // --- SONG METADATA & FAVORITES ---
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInitialMetadata(metadata: SongMetadata)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInitialMetadataList(metaList: List<SongMetadata>)

    // KORREKTUR: song_metadata (klein)
    @Query("UPDATE song_metadata SET lastPlayed = :timestamp, playCount = playCount + 1 WHERE songId = :id")
    suspend fun updateLastPlayed(id: Long, timestamp: Long)

    // KORREKTUR: song_metadata (klein)
    @Query("SELECT * FROM song_metadata WHERE lastPlayed < :threshold OR lastPlayed = 0")
    suspend fun getRediscoverMetadata(threshold: Long): List<SongMetadata>

    // KORREKTUR: song_metadata (klein)
    @Query("SELECT EXISTS(SELECT 1 FROM song_metadata WHERE songId = :id AND isFavorite = 1)")
    suspend fun isFavorite(id: Long): Boolean

    // KORREKTUR: song_metadata (klein)
    @Query("UPDATE song_metadata SET isFavorite = :isFav WHERE songId = :id")
    suspend fun setFavorite(id: Long, isFav: Boolean)

    // KORREKTUR: song_metadata (klein)
    @Query("SELECT songId FROM song_metadata WHERE isFavorite = 1")
    suspend fun getFavoriteSongIds(): List<Long>

    @Query("""
        SELECT songId FROM playback_history 
        GROUP BY songId 
        ORDER BY MAX(timestamp) DESC 
        LIMIT :limit
    """)
    fun getRecentPlayedIdsFlow(limit: Int): Flow<List<Long>>
}

// --- Hilfsklassen für Statistiken ---
data class SongTimeStat(val songId: Long, val totalTime: Long)
data class ArtistTimeStat(val artist: String, val totalTime: Long, val representativeSongId: Long)
data class AlbumTimeStat(val album: String, val artist: String, val totalTime: Long, val representativeSongId: Long)
data class MonthlyCount(val month: Int, val count: Long)