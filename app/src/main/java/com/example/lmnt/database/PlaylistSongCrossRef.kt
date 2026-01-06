package com.example.lmnt.database

import androidx.room.Entity

@Entity(tableName = "playlist_song_join", primaryKeys = ["playlistId", "songId"])
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songId: Long
)