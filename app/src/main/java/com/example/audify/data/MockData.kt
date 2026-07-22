package com.example.audify.data

import com.example.audify.model.Playlist
import com.example.audify.model.Podcast

object MockData {
    private val allPodcasts = listOf(
        Podcast(1, "Tech Today", "John Doe", "Discussions about the latest technology trends and innovations.", "15:30", "Tecnolog\u00eda"),
        Podcast(2, "Music Vibes", "DJ Alex", "A journey through the best electronic music of the month.", "22:15", "M\u00fasica"),
        Podcast(3, "Science Hour", "Dr. Sarah", "Exploring the wonders of science from physics to biology.", "18:45", "Ciencia"),
        Podcast(4, "Comedy Night", "Mike & Tom", "Laugh out loud with our weekly comedy sketches and interviews.", "12:20", "Comedia"),
        Podcast(5, "Book Club", "Emma Wilson", "Reviews and discussions of the most popular books this season.", "20:10", "Literatura"),
        Podcast(6, "Sports Talk", "Coach Mike", "In-depth analysis of the latest games and player performances.", "25:00", "Deportes"),
        Podcast(7, "Code & Coffee", "Gabriela D\u00edaz", "Tips y tricks para programadores en su d\u00eda a d\u00eda.", "10:45", "Tecnolog\u00eda"),
        Podcast(8, "Jazz Nights", "Gabriela D\u00edaz", "Los mejores soundtracks de jazz para programar.", "35:00", "M\u00fasica")
    )

    fun getPodcasts(): List<Podcast> = allPodcasts

    fun getUserPodcasts(): List<Podcast> = allPodcasts.filter { it.author == "Gabriela D\u00edaz" }

    fun getCategories(): List<String> = listOf("Tecnolog\u00eda", "M\u00fasica", "Ciencia", "Comedia", "Literatura", "Deportes")

    val playlists = mutableListOf(
        Playlist(1, "Favoritos", mutableListOf(1, 3, 5)),
        Playlist(2, "Para programar", mutableListOf(7, 2)),
        Playlist(3, "Aprender algo nuevo", mutableListOf(3, 1))
    )

    private var nextPlaylistId = 4

    fun createPlaylist(name: String): Playlist {
        val p = Playlist(nextPlaylistId++, name)
        playlists.add(p)
        return p
    }

    fun addPodcastToPlaylist(playlistId: Int, podcastId: Int) {
        playlists.find { it.id == playlistId }?.podcastIds?.add(podcastId)
    }

    fun removePodcastFromPlaylist(playlistId: Int, podcastId: Int) {
        playlists.find { it.id == playlistId }?.podcastIds?.remove(podcastId)
    }
}
