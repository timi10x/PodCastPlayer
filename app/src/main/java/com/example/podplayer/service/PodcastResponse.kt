package com.example.podplayer.service

data class PodcastResponse (
    val resultCount: Int,
    val results: List<ItunesPodcast>){

    //this data class here is a nested json,
    //so the results returning the list is like
    //"results" : [{and all the properties in here}]
    data class ItunesPodcast(
        val collectionCensoredName: String,
        val feedUrl: String,
        val artworkUrl100: String,
        val releaseDate: String
    )
}