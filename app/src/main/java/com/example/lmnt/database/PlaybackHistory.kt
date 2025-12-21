package com.example.lmnt.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_history")
data class PlaybackHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val songId: Long,
    val timestamp: Long = System.currentTimeMillis(), // Wann wurde es gehört?
    val year: Int, // Zum schnellen Filtern für "Wrapped"
    val month: Int
)