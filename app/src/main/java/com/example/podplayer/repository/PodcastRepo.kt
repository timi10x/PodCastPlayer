package com.example.podplayer.repository

import com.example.podplayer.model.Podcast

class PodcastRepo {
    fun getPodcast(
        feedUrl: String,
        callback: (Podcast?) -> Unit
    ) {
        Podcast(feedUrl, "No Name", "No description", "No image")
    }
}