package com.example.audify.model

data class Playlist(
    val id: Int,
    val name: String,
    val podcastIds: MutableList<Int> = mutableListOf()
)
