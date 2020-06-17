package com.example.podplayer.ui

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.method.ScrollingMovementMethod
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.podplayer.R
import com.example.podplayer.adapter.EpisodeListAdapter
import com.example.podplayer.service.PodplayMediaService
import com.example.podplayer.viewmodel.PodcastViewModel
import kotlinx.android.synthetic.main.activity_podcast.*
import kotlinx.android.synthetic.main.fragment_podcast_details.*
import java.lang.RuntimeException

class PodcastDetailsFragment : Fragment(), EpisodeListAdapter.EpisodeListAdapterListener {
    lateinit var podcastViewModel: PodcastViewModel
    private lateinit var episodeListAdapter: EpisodeListAdapter
    private var listener: OnPodcastDetailsListener? = null
    private var menuItem: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        setupViewModel()
    }

    private fun setupViewModel() {
        activity?.let {
            podcastViewModel = ViewModelProviders.of(activity!!)
                .get(PodcastViewModel::class.java)
        }
    }

    private fun updateMenuItem() {
        val viewData = podcastViewModel.activePodcastViewData ?: return
        menuItem?.title = if (viewData.subscribed)
            getString(R.string.unsubscribed) else getString(R.string.subscribe)
    }

    private fun updateControls() {
        val viewData = podcastViewModel.activePodcastViewData ?: return
        feedTitleTextView.text = viewData.feedTitle
        feedDescTextView.text = viewData.feedDesc
        activity?.let { activity ->
            Glide.with(activity).load(viewData.imageUrl).into(feedImageView)
        }

    }

    private fun setupControls() {
        //this is to allow the feed title to scroll, if it gets too long for its container
        feedDescTextView.movementMethod = ScrollingMovementMethod()
        //setup for the recyclerview
        episodeRecyclerView.setHasFixedSize(true)

        val layoutManager = LinearLayoutManager(activity)
        val dividerItemDecoration = DividerItemDecoration(
            podcastRecyclerView.context,
            layoutManager.orientation
        )
        episodeRecyclerView.addItemDecoration(dividerItemDecoration)
        //creating an episodeListAdapter with the list of episodes in the active
        //podcastViewData and assign it to episodeRecyclerView
        episodeListAdapter = EpisodeListAdapter(
            podcastViewModel.activePodcastViewData?.episodes, this
        )
        episodeRecyclerView.adapter = episodeListAdapter

    }


    override fun onStart() {
        super.onStart()

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onStop() {
        super.onStop()

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupControls()
        updateControls()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_details, menu)

        menuItem = menu?.findItem(R.id.menu_feed_action)
        updateMenuItem()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnPodcastDetailsListener) {
            listener = context
        } else {
            throw RuntimeException(
                context.toString()
                        + "must implement OnPodcastDetailsListener"
            )
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_feed_action -> {
                podcastViewModel.activePodcastViewData?.feedUrl?.let {
                    if (podcastViewModel.activePodcastViewData?.subscribed!!) {
                        listener?.onUnsubscribe()
                    } else {
                        listener?.onSubscribe()
                    }

                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        fun newInstance(): PodcastDetailsFragment {
            return PodcastDetailsFragment()
        }
    }

    interface OnPodcastDetailsListener {
        fun onSubscribe()
        fun onUnsubscribe()
        fun onShowEpisodePlayer(episodeViewData: PodcastViewModel.EpisodeViewData)
    }


    override fun onSelectEpisode(episodeViewData: PodcastViewModel.EpisodeViewData) {
        listener?.onShowEpisodePlayer(episodeViewData)
    }
}