package com.example.audify.data

import com.example.audify.model.Playlist
import com.example.audify.model.Podcast

object MockData {
    private val sampleAudioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"

    private val allPodcasts = listOf(
        Podcast(1, "Tech Today", "John Doe", "Discussions about the latest technology trends and innovations.", "15:30", "Tecnolog\u00eda", sampleAudioUrl),
        Podcast(2, "Music Vibes", "DJ Alex", "A journey through the best electronic music of the month.", "22:15", "M\u00fasica", sampleAudioUrl),
        Podcast(3, "Science Hour", "Dr. Sarah", "Exploring the wonders of science from physics to biology.", "18:45", "Ciencia", sampleAudioUrl),
        Podcast(4, "Comedy Night", "Mike & Tom", "Laugh out loud with our weekly comedy sketches and interviews.", "12:20", "Comedia", sampleAudioUrl),
        Podcast(5, "Book Club", "Emma Wilson", "Reviews and discussions of the most popular books this season.", "20:10", "Literatura", sampleAudioUrl),
        Podcast(6, "Sports Talk", "Coach Mike", "In-depth analysis of the latest games and player performances.", "25:00", "Deportes", sampleAudioUrl),
        Podcast(7, "Code & Coffee", "Gabriela D\u00edaz", "Tips y tricks para programadores en su d\u00eda a d\u00eda.", "10:45", "Tecnolog\u00eda", sampleAudioUrl),
        Podcast(8, "Jazz Nights", "Gabriela D\u00edaz", "Los mejores soundtracks de jazz para programar.", "35:00", "M\u00fasica", sampleAudioUrl)
    )

    fun getPodcasts(): List<Podcast> = allPodcasts

    fun getUserPodcasts(): List<Podcast> = allPodcasts.filter { it.author == "Gabriela D\u00edaz" }

    fun getCategories(): List<String> = listOf("Tecnolog\u00eda", "M\u00fasica", "Ciencia", "Comedia", "Literatura", "Deportes")

    private val _favoriteIds = mutableSetOf(1, 3, 5)

    fun getFavoriteIds(): Set<Int> = _favoriteIds

    fun isFavorite(podcastId: Int): Boolean = podcastId in _favoriteIds

    fun toggleFavorite(podcastId: Int) {
        if (_favoriteIds.contains(podcastId)) _favoriteIds.remove(podcastId)
        else _favoriteIds.add(podcastId)
    }

    fun getFavoritePodcasts(): List<Podcast> = allPodcasts.filter { it.id in _favoriteIds }

    val playlists = mutableListOf(
        Playlist(1, name = "Favoritos", podcastIds = mutableListOf(1, 3, 5)),
        Playlist(2, name = "Para programar", podcastIds = mutableListOf(7, 2)),
        Playlist(3, name = "Aprender algo nuevo", podcastIds = mutableListOf(3, 1))
    )

    private var nextPlaylistId = 4

    fun createPlaylist(name: String): Playlist {
        val p = Playlist(nextPlaylistId++, name = name)
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
