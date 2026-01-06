package com.example.lmnt.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PlaylistDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSongToPlaylist(crossRef: PlaylistSongCrossRef)

    @Query("SELECT * FROM Playlist")
    suspend fun getAllPlaylistsStatic(): List<Playlist>

    // Diese Query nutzt die exakten Namen deiner @Entity Definitionen
    @Query("""
        SELECT song_metadata.* FROM song_metadata 
        INNER JOIN playlist_song_join ON song_metadata.songId = playlist_song_join.songId 
        WHERE playlist_song_join.playlistId = :playlistId
    """)
    suspend fun getSongsForPlaylist(playlistId: Long): List<SongMetadata>
}