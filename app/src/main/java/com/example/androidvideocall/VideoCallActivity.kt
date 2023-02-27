package com.example.androidvideocall

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.androidvideocall.databinding.ActivityVideoCallBinding
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas


class VideoCallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoCallBinding

    private var agoraEngine: RtcEngine? = null

    private var localSurfaceView: SurfaceView? = null

    private var remoteSurfaceView: SurfaceView? = null

    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread { binding.setupRemoteVideo(uid) }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread { remoteSurfaceView?.isVisible = false }
        }
    }

    private val permissionCallback = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        val isCameraGranted = it.getOrDefault(Manifest.permission.CAMERA, false)
        val isAudioGranted = it.getOrDefault(Manifest.permission.RECORD_AUDIO, false)

        if (isCameraGranted && isAudioGranted) {
            binding.setupAgoraSdk()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.initBinding()
    }

    private fun ActivityVideoCallBinding.initBinding() {
        if (!allPermissionGranted()) {
            permissionCallback.launch(REQUESTED_PERMISSIONS)
        } else {
            setupAgoraSdk()
        }

        imgLeaveCall.setOnClickListener {
            leaveChannel()
        }
    }

    private fun ActivityVideoCallBinding.setupAgoraSdk() {
        try {
            val config = RtcEngineConfig()
            config.mContext = baseContext
            config.mAppId = BuildConfig.AGORA_APP_ID
            config.mEventHandler = mRtcEventHandler
            agoraEngine = RtcEngine.create(config)

            agoraEngine?.enableVideo()

            joinChannel()
        } catch (e: Exception) {
            Log.i("VideoCallActivity", e.message ?: "")
        }
    }

    private fun ActivityVideoCallBinding.setupRemoteVideo(uid: Int) {
        remoteSurfaceView = SurfaceView(baseContext)
        remoteSurfaceView?.setZOrderMediaOverlay(true)
        remoteVideoViewContainer.addView(remoteSurfaceView)
        agoraEngine?.setupRemoteVideo(
            VideoCanvas(
                remoteSurfaceView,
                VideoCanvas.RENDER_MODE_FIT,
                uid
            )
        )
        remoteSurfaceView?.isVisible = true
    }

    private fun ActivityVideoCallBinding.setupLocalVideo() {
        localSurfaceView = SurfaceView(baseContext)
        localVideoViewContainer.addView(localSurfaceView)

        agoraEngine?.setupLocalVideo(
            VideoCanvas(
                localSurfaceView,
                VideoCanvas.RENDER_MODE_HIDDEN,
                0
            )
        )
    }

    private fun ActivityVideoCallBinding.joinChannel() {
        val options = ChannelMediaOptions()

        options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER

        setupLocalVideo()
        localSurfaceView?.isVisible = true

        agoraEngine?.startPreview()

        agoraEngine?.joinChannel(BuildConfig.AGORA_TOKEN, BuildConfig.AGORA_CHANNEL_NAME, 0, options)
    }

    private fun leaveChannel() {
        agoraEngine?.leaveChannel()

        if (remoteSurfaceView != null) remoteSurfaceView?.isVisible = false
        if (localSurfaceView != null) localSurfaceView?.isVisible = false

        finish()
    }

    private fun allPermissionGranted(): Boolean = REQUESTED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this@VideoCallActivity, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        agoraEngine?.stopPreview()
        agoraEngine?.leaveChannel()

        Thread {
            RtcEngine.destroy()
            agoraEngine = null
        }.start()
    }

    companion object {
        private val REQUESTED_PERMISSIONS = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
    }
}