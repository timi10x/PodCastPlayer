package com.example.podplayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.example.podplayer.model.Episode
import com.example.podplayer.model.Podcast
import com.example.podplayer.repository.PodcastRepo
import com.example.podplayer.utils.DateUtils
import java.util.*

class PodcastViewModel(application: Application) : AndroidViewModel
    (application) {

    var podcastRepo: PodcastRepo? = null
    var activePodcastViewData: PodcastViewData? = null
    private var activePodcast: Podcast? = null
    var livePodcastData: LiveData<List<SearchViewModel.PodcastSummaryViewData>>? = null

    var activeEpisodeViewData: EpisodeViewData? = null
    fun getPodcast(
        podcastSummaryViewData: SearchViewModel.PodcastSummaryViewData,
        callback: (PodcastViewData?) ->
        Unit
    ) {

        val repo = podcastRepo ?: return
        val feedUrl = podcastSummaryViewData.feedUrl ?: return

        repo.getPodcast(feedUrl) {
            it?.let {
                it.feedTitle = podcastSummaryViewData.name ?: ""
                it.imageUrl = podcastSummaryViewData.imageUrl ?: ""
                activePodcastViewData = podcastToPodcastView(it)
                activePodcast = it
                callback(activePodcastViewData)
            }
        }
    }

    fun getPodcasts(): LiveData<List<SearchViewModel.PodcastSummaryViewData>>? {
        val repo = podcastRepo
            ?: return null
        if (livePodcastData == null) {
            val liveData = repo.getAll()
            livePodcastData = Transformations.map(liveData) { podcastList ->
                podcastList.map { podcast ->
                    podcastToSummaryView(podcast)
                }
            }
        }
        return livePodcastData
    }

    fun saveActivePodcast() {
        val repo = podcastRepo ?: return
        activePodcast?.let {
            it.episodes = it.episodes.drop(1)
            repo.save(it)
        }
    }

    private fun podcastToSummaryView(podcast: Podcast): SearchViewModel.PodcastSummaryViewData {
        return SearchViewModel.PodcastSummaryViewData(
            podcast.feedTitle,
            DateUtils.dateToShortDate(podcast.lastUpdated),
            podcast.imageUrl,
            podcast.feedUrl
        )
    }

    private fun podcastToPodcastView(podcast: Podcast): PodcastViewData {
        return PodcastViewData(
            podcast.id != null,
            podcast.feedTitle,
            podcast.feedUrl,
            podcast.feedDesc,
            podcast.imageUrl,
            episodesToEpisodesView(podcast.episodes)
        )
    }

    fun setActivePodcast(
        feedUrl: String,
        callback: (SearchViewModel.PodcastSummaryViewData?) -> Unit
    ) {
        val repo = podcastRepo ?: return
        repo.getPodcast(
            feedUrl
        ) { podcast ->
            if (podcast == null) {
                callback(null)
            } else {
                activePodcastViewData = podcastToPodcastView(podcast)
                activePodcast = podcast
                callback(podcastToSummaryView(podcast))
            }
        }
    }

    fun deleteActivePodcast() {
        val repo = podcastRepo ?: return
        activePodcast?.let {
            repo.delete(it)
        }
    }

    private fun episodesToEpisodesView(episodes: List<Episode>): List<EpisodeViewData> {
        return episodes.map {
            val isVideo = it.mimeType.startsWith("video")
            EpisodeViewData(
                it.guid, it.title, it.description, it.mediaUrl,
                it.releaseDate, it.duration, isVideo
            )
        }
    }

    data class PodcastViewData(
        var subscribed: Boolean = false,
        var feedTitle: String? = "",
        var feedUrl: String? = "",
        var feedDesc: String? = "",
        var imageUrl: String? = "",
        var episodes: List<EpisodeViewData>
    )

    data class EpisodeViewData(
        var guid: String? = "",
        var title: String? = "",
        var description: String? = "",
        var mediaUrl: String? = "",
        var releaseDate: Date? = null,
        var duration: String? = "",
        var isVideo: Boolean = false
    )
}
