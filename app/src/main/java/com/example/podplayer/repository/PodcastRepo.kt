package com.example.podplayer.repository

import androidx.lifecycle.LiveData
import com.example.podplayer.db.PodcastDao
import com.example.podplayer.model.Episode
import com.example.podplayer.model.Podcast
import com.example.podplayer.service.FeedService
import com.example.podplayer.service.RssFeedResponse
import com.example.podplayer.service.RssFeedService
import com.example.podplayer.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class PodcastRepo(private var feedService: FeedService, private var podcastDao: PodcastDao) {
    val rssFeedService = RssFeedService()
    fun getPodcast(feedUrl: String, callback: (Podcast?) -> Unit) {

        GlobalScope.launch {

            val podcast = podcastDao.loadPodcast(feedUrl)

            if (podcast != null) {
                podcast.id?.let {
                    podcast.episodes = podcastDao.loadEpisodes(it)
                    GlobalScope.launch(Dispatchers.Main) {
                        callback(podcast)
                    }
                }
            } else {

                feedService.getFeed(feedUrl) { feedResponse ->

                    if (feedResponse == null) {
                        GlobalScope.launch(Dispatchers.Main) {
                            callback(null)
                        }
                    } else {
                        val podcast = rssResponseToPodcast(feedUrl, "", feedResponse)
                        GlobalScope.launch(Dispatchers.Main) {
                            callback(podcast)
                        }
                    }
                }
            }
        }

    }

    private fun getNewEpisodes(
        localPodcast: Podcast,
        callBack: (List<Episode>) -> Unit
    ) {
        feedService.getFeed(localPodcast.feedUrl) { response ->
            if (response != null) {
                val remotePodcast = rssResponseToPodcast(
                    localPodcast.feedUrl,
                    localPodcast.imageUrl, response
                )
                remotePodcast?.let {
                    val localEpisodes = podcastDao.loadEpisodes(localPodcast.id!!)
                    val newEpisodes = remotePodcast.episodes.filter { episode ->
                        localEpisodes.find { episode.guid == it.guid } == null
                    }
                    callBack(newEpisodes)
                }
            } else {
                callBack(listOf())
            }
        }
    }

    private fun saveNewEpisodes(podcastId: Long, episodes: List<Episode>) {
        GlobalScope.launch {
            for (episode in episodes) {
                episode.podcastId = podcastId
                podcastDao.insertEpisode(episode)
            }
        }
    }

    fun delete(podcast: Podcast) {
        GlobalScope.launch { podcastDao.deletePodcast(podcast) }
    }

    private fun rssItemsToEpisodes(episodeResponses: List<RssFeedResponse.EpisodeResponse>): List<Episode> {
        return episodeResponses.map {
            Episode(
                it.guid ?: "",
                null,
                it.title ?: "",
                it.description ?: "",
                it.url ?: "",
                it.type ?: "",
                DateUtils.xmlDateToDate(it.pubDate!!),
                it.duration ?: ""
            )
        }
    }

    fun getAll(): LiveData<List<Podcast>> {
        return podcastDao.loadPodcasts()
    }

    fun save(podcast: Podcast) {
        GlobalScope.launch {
            val podcastId = podcastDao.insertPodcast(podcast)
            for (episode in podcast.episodes) {
                episode.podcastId = podcastId
                podcastDao.insertEpisode(episode)
            }
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
            null,
            feedUrl,
            rssResponse.title,
            description,
            imageUrl,
            rssResponse.lastUpdated,
            episodes = rssItemsToEpisodes(items)
        )
    }
    fun updatePodcastEpisodes(callBack: (List<PodcastUpdateInfo>) -> Unit){
        val updatePodcasts: MutableList<PodcastUpdateInfo> = mutableListOf()
        val podcasts = podcastDao.loadPodcastsStatic()
        var processCount = podcasts.count()

        for (podcast in podcasts){
            getNewEpisodes(podcast){
                newEpisodes ->
                if (newEpisodes.count() > 0){
                    saveNewEpisodes(podcast.id!!, newEpisodes)
                    updatePodcasts.add(
                        PodcastUpdateInfo(podcast.feedUrl,
                    podcast.feedTitle, newEpisodes.count())
                    )
                }
                processCount--
                if (processCount == 0){
                    callBack(updatePodcasts)
                }
            }
        }
    }

    class PodcastUpdateInfo(val feedUrl: String, val name: String, val newCount: Int)

}