package com.example.lmnt.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_history")
data class PlaybackHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val songId: Long,
    val songTitle: String,
    val artist: String,
    val duration: Long, // Diese Zeile hinzufügen!
    val timestamp: Long,
    val day: Int,
    val month: Int,
    val year: Int

)