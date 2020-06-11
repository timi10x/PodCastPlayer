package com.example.podplayer.repository

import com.example.podplayer.model.Episode
import com.example.podplayer.model.Podcast
import com.example.podplayer.service.FeedService
import com.example.podplayer.service.RssFeedResponse
import com.example.podplayer.service.RssFeedService
import com.example.podplayer.utils.DateUtils

class PodcastRepo(private var feedService: FeedService) {
    val rssFeedService = RssFeedService()
    fun getPodcast(
        feedUrl: String,
        callback: (Podcast?) -> Unit
    ) {
        Podcast(feedUrl, "No Name", "No description", "No image")
        rssFeedService.getFeed(feedUrl) {

        }
    }

    private fun rssItemsToEpisodes(episodeResponses: List<RssFeedResponse.EpisodeResponse>): List<Episode> {
        return episodeResponses.map {
            Episode(
                it.guid ?: "",
                it.title ?: "",
                it.description ?: "",
                it.url ?: "",
                it.type ?: "",
                DateUtils.xmlDateToDate(it.pubDate!!),
                it.duration ?: ""
            )
        }
    }

    private fun rssResponseToPodcast(
        feedUrl: String,
        imageUrl: String,
        rssResponse: RssFeedResponse
    ): Podcast? {
        // assigning the list of episodes to items provided it's not null
        //otherwise, method returns null
        val items = rssResponse.episodes ?: return null
        // if description is empty, the desc property is set to the response summary
        val description = if (rssResponse.description == "")
            rssResponse.summary else rssResponse.description
        // create a new podcast object using the response data and then return it to the caller
        return Podcast(
            feedUrl,
            rssResponse.title,
            description,
            imageUrl,
            rssResponse.lastUpdated,
            episodes = rssItemsToEpisodes(items)
        )
    }

}