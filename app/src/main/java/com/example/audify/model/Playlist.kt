package com.example.audify.model

data class Playlist(
    val id: Int,
    val supabaseId: String = "",
    val name: String,
    val podcastCount: Int = 0,
    val podcastIds: MutableList<Int> = mutableListOf()
)
