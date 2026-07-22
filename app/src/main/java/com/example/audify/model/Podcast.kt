package com.example.audify.model

data class Podcast(
    val id: Int,
    val title: String,
    val author: String,
    val description: String,
    val duration: String,
    val category: String = "General",
    val audioUrl: String = ""
)
