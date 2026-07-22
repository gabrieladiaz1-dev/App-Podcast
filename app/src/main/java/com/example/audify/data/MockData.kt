package com.example.audify.data

import com.example.audify.model.Podcast

object MockData {
    fun getPodcasts(): List<Podcast> = listOf(
        Podcast(1, "Tech Today", "John Doe", "Discussions about the latest technology trends and innovations.", "15:30"),
        Podcast(2, "Music Vibes", "DJ Alex", "A journey through the best electronic music of the month.", "22:15"),
        Podcast(3, "Science Hour", "Dr. Sarah", "Exploring the wonders of science from physics to biology.", "18:45"),
        Podcast(4, "Comedy Night", "Mike & Tom", "Laugh out loud with our weekly comedy sketches and interviews.", "12:20"),
        Podcast(5, "Book Club", "Emma Wilson", "Reviews and discussions of the most popular books this season.", "20:10"),
        Podcast(6, "Sports Talk", "Coach Mike", "In-depth analysis of the latest games and player performances.", "25:00")
    )
}
