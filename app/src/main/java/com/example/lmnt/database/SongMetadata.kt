package com.example.lmnt.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "song_metadata")
data class SongMetadata(
    @PrimaryKey val songId: Long,
    val lastPlayed: Long = 0L,
    val isFavorite: Boolean = false,
    val playCount: Int = 0
)