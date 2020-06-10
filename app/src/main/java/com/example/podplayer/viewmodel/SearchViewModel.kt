package com.example.podplayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.podplayer.repository.ItunesRepo
import com.example.podplayer.service.PodcastResponse

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    var itunesRepo: ItunesRepo? = null

    data class PodcastSummaryViewData(
        var name: String? = "",
        var lastUpdated: String? = "",
        var imageUrl: String? = "",
        var feedUrl: String? = ""
    )

    private fun itunesPodcaastToPodcastSummaryView(itunesPodcast: PodcastResponse.ItunesPodcast):
            PodcastSummaryViewData {
        return PodcastSummaryViewData(
            itunesPodcast.collectionCensoredName,
            itunesPodcast.releaseDate,
            itunesPodcast.artworkUrl30,
            itunesPodcast.feedUrl
        )
    }

    fun searchPodcasts(
        term: String,
        callback: (List<PodcastSummaryViewData>) -> Unit
    ) {
        itunesRepo?.searchByTerm(term) { results ->
            if (results == null) {
                callback(emptyList())
            } else {
                val searchViews = results.map { podcast ->
                    itunesPodcaastToPodcastSummaryView(podcast)
                }
                callback(searchViews)
            }
        }
    }
}