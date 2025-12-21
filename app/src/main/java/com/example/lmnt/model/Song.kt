package com.example.lmnt

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val uri: String,
    val trackNumber: Int,
    val discNumber: Int,
    val artworkUri: String,
    val duration: Int
)
