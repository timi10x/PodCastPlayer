package com.example.podplayer.service

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat

class PodplayMediaService : MediaBrowserServiceCompat(){
    private lateinit var mediaSession : MediaSessionCompat
    override fun onCreate() {
        super.onCreate()
        createMediaSession()
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        if (parentId.equals(PODPLAY_EMPTY_ROOT_MEDIA_ID)){
            result.sendResult(null)
        }
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        return BrowserRoot(
            PODPLAY_EMPTY_ROOT_MEDIA_ID,
            null
        )
    }

    private fun createMediaSession(){
        mediaSession = MediaSessionCompat(this, "PodplayMediaService")

        //this setFlags indicates which action the media session supports
        //it allows it to respond to any events
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)

        //the unique token for the media session is retrieved and applied as the seesion token on the podplayermediaservice
        //and it links the service to the media session
        sessionToken = mediaSession.sessionToken

        val callback = PodplayMediaCallback(this, mediaSession)
        mediaSession.setCallback(callback)
    }

    companion object{
        private const val PODPLAY_EMPTY_ROOT_MEDIA_ID = "podplay_empty_root_media_id"
    }
}