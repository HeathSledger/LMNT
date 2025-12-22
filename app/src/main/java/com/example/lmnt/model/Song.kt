package com.example.lmnt

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val uri: String,
    val artworkUri: String, // Hierher verschoben, passend zum MusicLoader
    val trackNumber: Int,
    val discNumber: Int,
    val duration: Int
) : Parcelable