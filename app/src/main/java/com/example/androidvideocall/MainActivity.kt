package com.example.androidvideocall

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.SurfaceView
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas


class MainActivity : AppCompatActivity() {

    private val appId = "bd7b75448cde47899df88d98476ed950"

    // Fill the channel name.
    private val channelName = "test"

    // Fill the temp token generated on Agora Console.
    private val token = "007eJxTYPDj9xb5tqKasTO188S6lSVq+SIPJpWXZF6rzvr5oMg3dIcCQ1KKeZK5qYmJRXJKqom5haVlSpqFRYqlhYm5WWqKpalBw8s/yQ2BjAwWdy0ZGRkgEMRnYShJLS5hYAAAVyUgNg=="

    // An integer that identifies the local user.
    private val uid = 0
    private var isJoined = false

    private var agoraEngine: RtcEngine? = null

    //SurfaceView to render local video in a Container.
    private var localSurfaceView: SurfaceView? = null

    //SurfaceView to render Remote video in a Container.
    private var remoteSurfaceView: SurfaceView? = null

    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        // Listen for the remote host joining the channel to get the uid of the host.
        override fun onUserJoined(uid: Int, elapsed: Int) {

            // Set the remote video view
            runOnUiThread { setupRemoteVideo(uid) }
        }

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            isJoined = true
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread { remoteSurfaceView!!.visibility = View.GONE }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!allPermissionGranted()) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, PERMISSION_REQ_ID)
        }
        setupVideoSDKEngine();
    }

    private fun setupVideoSDKEngine() {
        try {
            val config = RtcEngineConfig()
            config.mContext = baseContext
            config.mAppId = appId
            config.mEventHandler = mRtcEventHandler
            agoraEngine = RtcEngine.create(config)
            // By default, the video module is disabled, call enableVideo to enable it.
            agoraEngine?.enableVideo()
        } catch (e: Exception) {

        }
    }

    private fun setupRemoteVideo(uid: Int) {
        val container = findViewById<FrameLayout>(R.id.remote_video_view_container)
        remoteSurfaceView = SurfaceView(baseContext)
        remoteSurfaceView?.setZOrderMediaOverlay(true)
        container.addView(remoteSurfaceView)
        agoraEngine?.setupRemoteVideo(
            VideoCanvas(
                remoteSurfaceView,
                VideoCanvas.RENDER_MODE_FIT,
                uid
            )
        )
        remoteSurfaceView?.visibility = View.VISIBLE
    }

    private fun setupLocalVideo() {
        val container = findViewById<FrameLayout>(R.id.local_video_view_container)

        localSurfaceView = SurfaceView(baseContext)
        container.addView(localSurfaceView)

        agoraEngine?.setupLocalVideo(
            VideoCanvas(
                localSurfaceView,
                VideoCanvas.RENDER_MODE_HIDDEN,
                0
            )
        )
    }

    fun joinChannel(view: View?) {
        if (allPermissionGranted()) {
            val options = ChannelMediaOptions()

            options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
            options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER

            setupLocalVideo()
            localSurfaceView?.visibility = View.VISIBLE

            agoraEngine?.startPreview()

            agoraEngine?.joinChannel(token, channelName, uid, options)
        } else {
            Toast.makeText(applicationContext, "Permissions was not granted", Toast.LENGTH_SHORT)
                .show()
        }
    }

    fun leaveChannel(view: View?) {
        if (!isJoined) {
//            showMessage("Join a channel first")
        } else {
            agoraEngine!!.leaveChannel()
//            showMessage("You left the channel")
            // Stop remote video rendering.
            if (remoteSurfaceView != null) remoteSurfaceView?.visibility = View.GONE
            // Stop local video rendering.
            if (localSurfaceView != null) localSurfaceView?.visibility = View.GONE
            isJoined = false
        }
    }

    private fun allPermissionGranted(): Boolean = REQUESTED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this@MainActivity, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        agoraEngine?.stopPreview()
        agoraEngine?.leaveChannel()

        // Destroy the engine in a sub-thread to avoid congestion
        Thread {
            RtcEngine.destroy()
            agoraEngine = null
        }.start()
    }

    companion object {
        private const val PERMISSION_REQ_ID = 22
        private val REQUESTED_PERMISSIONS = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.RECORD_AUDIO
        )
    }
}