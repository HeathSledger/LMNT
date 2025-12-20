package com.example.lmnt.model

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val artworkUri: String,
    val songCount: Int
)