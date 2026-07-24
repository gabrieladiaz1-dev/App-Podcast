package com.example.audify.model

data class Podcast(
    val id: Int = 0,
    val supabaseId: String = "",
    val title: String = "",
    val author: String = "",
    val description: String = "",
    val duration: String = "",
    val category: String = "General",
    val audioUrl: String = "",
    val coverUrl: String? = null,
    val userId: String = "",
    val approved: Boolean = false
)
