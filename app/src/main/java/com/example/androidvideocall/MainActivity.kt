package com.example.androidvideocall

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.androidvideocall.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnJoinVideoCall.setOnClickListener {
            navigateToVideoCall()
        }
    }

    private fun navigateToVideoCall() {
        val intent = Intent(this, VideoCallActivity::class.java)
        startActivity(intent)
    }
}