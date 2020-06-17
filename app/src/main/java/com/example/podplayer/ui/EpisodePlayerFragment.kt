package com.example.podplayer.ui

import android.animation.ValueAnimator
import android.content.ComponentName
import android.graphics.Color
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.format.DateUtils
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.Glide
import com.example.podplayer.R
import com.example.podplayer.service.PodplayMediaCallback
import com.example.podplayer.service.PodplayMediaCallback.Companion.CMD_CHANGESPEED
import com.example.podplayer.service.PodplayMediaCallback.Companion.CMD_EXTRA_SPEED
import com.example.podplayer.service.PodplayMediaService
import com.example.podplayer.utils.HtmlUtils
import com.example.podplayer.viewmodel.PodcastViewModel
import kotlinx.android.synthetic.main.fragment_episode_player.*

class EpisodePlayerFragment : Fragment() {

    private lateinit var podcastViewModel: PodcastViewModel
    private lateinit var mediaBrowser: MediaBrowserCompat
    private var mediaControllerCallback: MediaControllerCallback? = null
    private var episodeDuration: Long = 0
    private var playerSpeed: Float = 1.0f
    private var draggingScrubber: Boolean = false
    private var progressAnimator: ValueAnimator? = null
    private var mediaSession: MediaSessionCompat? = null
    private var mediaPlayer: MediaPlayer? = null
    private var playOnPrepare: Boolean = false
    private var isVideo: Boolean = false

    companion object {
        fun newInstance(): EpisodePlayerFragment {
            return EpisodePlayerFragment()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        setupViewModel()
        if (!isVideo){
            initMediaBrowser()
    }
    }

    private fun startPlaying(
        episodeViewData: PodcastViewModel.EpisodeViewData
    ) {
        val fragmentActivity = activity as FragmentActivity
        val controller = MediaControllerCompat.getMediaController(fragmentActivity)
        val viewData = podcastViewModel.activePodcastViewData ?: return
        val bundle = Bundle()
        bundle.putString(
            MediaMetadataCompat.METADATA_KEY_TITLE,
            episodeViewData.title
        )
        bundle.putString(
            MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST,
            viewData.feedTitle
        )
        bundle.putString(
            MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,
            viewData.imageUrl
        )
        controller.transportControls.playFromUri(
            Uri.parse(
                episodeViewData.mediaUrl
            ), bundle
        )
    }

    private fun tooglePlayPause() {
        playOnPrepare = true
        val fragmentActivity = activity as FragmentActivity
        val controller =
            MediaControllerCompat.getMediaController(fragmentActivity)
        if (controller.playbackState != null) {
            if (controller.playbackState.state ==
                PlaybackStateCompat.STATE_PLAYING
            ) {
                controller.transportControls.pause()
            } else {
                podcastViewModel.activeEpisodeViewData?.let {
                    startPlaying(it)
                }
            }
        } else {
            playToggleButton.setOnClickListener {
                podcastViewModel.activeEpisodeViewData?.let {
                    startPlaying(it)
                }
            }
        }
    }

    private fun initVideoPlayer() {
        videoSurfaceView.visibility = View.VISIBLE
        val surfaceHolder = videoSurfaceView.holder
        surfaceHolder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                initMediaPlayer()
                mediaPlayer?.setDisplay(holder)
            }

            override fun surfaceChanged(var1: SurfaceHolder, var2: Int, var3: Int, var4: Int) {}
            override fun surfaceDestroyed(var1: SurfaceHolder) {}
        })
    }

    private fun setupControls() {
        playToggleButton.setOnClickListener {
            tooglePlayPause()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            speedButton.setOnClickListener { changeSpeed() }
        } else {
            speedButton.visibility = View.INVISIBLE
        }
        forwardButton.setOnClickListener {
            seekBy(30)
        }
        replayButton.setOnClickListener {
            seekBy(-10)
        }
        seekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    currentTimeTextView.text = DateUtils.formatElapsedTime(
                        (progress / 1000).toLong()
                    )
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    draggingScrubber = true
                    val fragmentActivity = activity as FragmentActivity
                    val controller =
                        MediaControllerCompat.getMediaController(fragmentActivity)
                    if (controller.playbackState != null) {
                        controller.transportControls.seekTo(seekBar!!.progress.toLong())
                    } else {
                        seekBar!!.progress = 0
                    }
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {

                }

            }
        )
    }

    private fun seekBy(seconds: Int) {
        val fragmentActivity = activity as FragmentActivity
        val controller = MediaControllerCompat.getMediaController(fragmentActivity)
        val newPosition =
            controller.playbackState.position + seconds * 1000
        controller.transportControls.seekTo(newPosition)
    }

    private fun changeSpeed() {
        playerSpeed += 0.25f
        if (playerSpeed > 2.0f) {
            playerSpeed = 0.75f
        }
        val bundle = Bundle()
        bundle.putFloat(CMD_EXTRA_SPEED, playerSpeed)
        val fragmentActivity = activity as FragmentActivity
        val controller = MediaControllerCompat.getMediaController(fragmentActivity)
        controller.sendCommand(CMD_CHANGESPEED, bundle, null)
        speedButton.text = "${playerSpeed}x"
    }

    private fun handleStateChange(state: Int, position: Long, speed: Float) {
        val isPlaying = state == PlaybackStateCompat.STATE_PLAYING
        playToggleButton.isActivated = isPlaying
        val progress =
            position.toInt()
        seekBar.progress = progress
        speedButton.text = "${playerSpeed}x"
        if (isPlaying) {
            if (isVideo){
                setupVideoUI()
            }
            animateScrubber(progress, speed)
        }
        progressAnimator?.let {
            it.cancel()
            progressAnimator = null
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_episode_player, container, false)
    }

    private fun updateControlsFromMetadata(metadata: MediaMetadataCompat) {
        episodeDuration =
            metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
        endTimeTextView.text = DateUtils.formatElapsedTime(episodeDuration / 1000)
        seekBar.max = episodeDuration.toInt()
    }

    private fun initMediaSession() {
        if (mediaSession == null) {
            mediaSession = MediaSessionCompat(activity, "EpisodePlayerFragment")
            mediaSession?.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            mediaSession?.setMediaButtonReceiver(null)
        }
        registerMediaController(mediaSession!!.sessionToken)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (isVideo){
            initMediaSession()
            initVideoPlayer()
        }
        updateControls()
        setupControls()
    }

    private fun setupVideoUI(){
        episodeDescTextView.visibility = View.INVISIBLE
        headerView.visibility = View.INVISIBLE
        val activity = activity as AppCompatActivity
        activity.supportActionBar?.hide()
        playerControls.setBackgroundColor(Color.argb(255/2, 0, 0, 0))
    }

    private fun setupViewModel() {
        val fragmentActivity =
            activity as FragmentActivity
        podcastViewModel = ViewModelProviders.of(fragmentActivity).get(
            PodcastViewModel::class.java
        )
        isVideo = podcastViewModel.activeEpisodeViewData?.isVideo ?: false
    }

    private fun updateControls() {
        episodeTitleTextView.text = podcastViewModel.activeEpisodeViewData?.title

        val htmlDesc =
            podcastViewModel.activeEpisodeViewData?.description ?: ""
        val descSpan = HtmlUtils.htmlToSpannable(htmlDesc)
        episodeDescTextView.text = descSpan
        episodeDescTextView.movementMethod = ScrollingMovementMethod()

        val fragmentActivity = activity as FragmentActivity
        Glide.with(fragmentActivity)
            .load(podcastViewModel.activePodcastViewData?.imageUrl)
            .into(episodeImageView)
        speedButton.text = "${playerSpeed}x"
        mediaPlayer?.let {
            updateControlsFromController()
        }
    }

    private fun setSurfaceSize() {
        val mediaPlayer = mediaPlayer ?: return
        val videoWidth = mediaPlayer.videoWidth
        val videoHeight = mediaPlayer.videoHeight
        val parent = videoSurfaceView.parent as View
        val containerWidth = parent.width
        val containerHeight = parent.height
        val layoutAspectRatio = containerWidth.toFloat() / containerHeight
        val videoAspectRatio = videoWidth.toFloat() / videoHeight
        val layoutParams = videoSurfaceView.layoutParams
        if (videoAspectRatio > layoutAspectRatio) {
            layoutParams.height = (containerWidth / videoAspectRatio).toInt()
        } else {
            layoutParams.width = (containerHeight * videoAspectRatio).toInt()
        }
        videoSurfaceView.layoutParams = layoutParams
    }

    private fun initMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer()
            mediaPlayer?.let {
                it.setAudioStreamType(AudioManager.STREAM_MUSIC)
                it.setDataSource(podcastViewModel.activeEpisodeViewData?.mediaUrl)
                it.setOnPreparedListener {
                    val fragmentActivity = activity as FragmentActivity
                    val episodeMediaCallback =
                        PodplayMediaCallback(fragmentActivity, mediaSession!!, it)
                    mediaSession!!.setCallback(episodeMediaCallback)
                    setSurfaceSize()
                    if (playOnPrepare) {
                        tooglePlayPause()
                    }
                }
                it.prepareAsync()
            }
        } else {
            setSurfaceSize()
        }
    }

    override fun onStart() {
        super.onStart()
        if (!isVideo){
            if (mediaBrowser.isConnected) {
                val fragmentActivity = activity as FragmentActivity
                if (MediaControllerCompat.getMediaController(fragmentActivity) == null) {
                    registerMediaController(mediaBrowser.sessionToken)
                }
                updateControlsFromController()
            } else {
                mediaBrowser.connect()
            }
    }
    }

    override fun onStop() {
        super.onStop()
        progressAnimator?.cancel()
        val fragmentActivity = activity as FragmentActivity
        if (MediaControllerCompat.getMediaController(fragmentActivity) != null) {
            mediaControllerCallback?.let {
                MediaControllerCallback()?.let {
                    MediaControllerCompat.getMediaController(fragmentActivity)
                        .unregisterCallback(it)
                }
            }
        }
        if (!fragmentActivity.isChangingConfigurations){
            mediaPlayer!!.release()
            mediaPlayer = null
        }
    }

    private fun updateControlsFromController() {
        val fragmentActivity = activity as FragmentActivity
        val controller =
            MediaControllerCompat.getMediaController(fragmentActivity)
        if (controller != null) {
            val metadata = controller.metadata
            if (metadata != null) {
                handleStateChange(
                    controller.playbackState.state,
                    controller.playbackState.position,
                    playerSpeed
                )
                updateControlsFromMetadata(controller.metadata)
            }
        }
    }

    private fun initMediaBrowser() {
        val fragmentActivity = activity as FragmentActivity
        mediaBrowser = MediaBrowserCompat(
            fragmentActivity,
            ComponentName(fragmentActivity, PodplayMediaService::class.java),
            MediaBrowserCallBacks(),
            null
        )
    }

    private fun animateScrubber(progress: Int, speed: Float) {
        val timeRemaining = ((episodeDuration - progress) / speed).toInt()
        if (timeRemaining < 0) {
            return
        }
        progressAnimator = ValueAnimator.ofInt(
            progress, episodeDuration.toInt()
        )
        progressAnimator?.let { animator ->
            animator.duration = timeRemaining.toLong()
            animator.interpolator = LinearInterpolator()
            animator.addUpdateListener {
                if (draggingScrubber) {
                    animator.cancel()
                } else {
                    seekBar.progress = animator.animatedValue as Int
                }
            }
            animator.start()
        }
    }

    private fun registerMediaController(token: MediaSessionCompat.Token) {
        val fragmentActivity = activity as FragmentActivity
        val mediaController = MediaControllerCompat(fragmentActivity, token)

        MediaControllerCompat.setMediaController(fragmentActivity, mediaController)

        mediaControllerCallback = MediaControllerCallback()
        mediaController.registerCallback(mediaControllerCallback!!)
    }


    inner class MediaControllerCallback : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            println(
                "metadata changed to ${
                metadata?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI)
                }"
            )
            metadata?.let { updateControlsFromMetadata(it) }
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            val state = state ?: return
            handleStateChange(state.state, state.position, state.playbackSpeed)
        }
    }

    inner class MediaBrowserCallBacks : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            super.onConnected()
            registerMediaController(mediaBrowser.sessionToken)
            updateControlsFromController()
        }

        override fun onConnectionSuspended() {
            super.onConnectionSuspended()
            println("onConnectionSuspended")
        }

        override fun onConnectionFailed() {
            super.onConnectionFailed()
            println("onConnectionFailed")
        }
    }


}