package com.example.videoimage_avatar

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.example.videoimage_avatar.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var uri: Uri? = null

    val sharedPrefs: SharedPreferences by lazy {
        getSharedPreferences("smartlab", Context.MODE_PRIVATE)
    }

    fun saveAvatar(avatarUri: Uri) {
        sharedPrefs.edit().putString("avatar", avatarUri.toString()).apply()
    }

    fun saveIsVideo(isVideo: Boolean) {
        sharedPrefs.edit().putBoolean("isVideo", isVideo).apply()
    }

    fun getAvatar(): Uri {
        return sharedPrefs.getString("avatar", "")?.toUri() ?: "".toUri()
    }

    fun getIsVideo(): Boolean {
        return sharedPrefs.getBoolean("isVideo", false)
    }

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) {
        if (it) {
            uri?.let { pictureUri ->
                saveAvatar(pictureUri)
                setImageAvatar(pictureUri)
            }
        }
    }

    private fun setImageAvatar(uri: Uri) {
        saveIsVideo(false)
        saveAvatar(uri)
        Log.d("main", "imageUri: $uri")
        binding.videoView.visibility = View.GONE
        binding.imageView.visibility = View.VISIBLE
        Glide.with(this).load(uri).into(binding.imageView)
    }

    private fun setVideoAvatar(uri: Uri) {
        saveIsVideo(true)
        saveAvatar(uri)
        binding.imageView.visibility = View.GONE
        binding.videoView.visibility = View.VISIBLE
        binding.videoView.apply {
            setVideoURI(uri)
            setOnPreparedListener { mediaPlayer ->
                mediaPlayer.setVolume(0f, 0f)
                mediaPlayer.start()
            }
            setOnCompletionListener { mediaPlayer ->
                mediaPlayer.stop()
                mediaPlayer.release()
            }
            setOnCompletionListener {
                binding.videoView.start()
            }
        }
        binding.videoView.start()
    }

    private val takeVideo = registerForActivityResult(ActivityResultContracts.CaptureVideo()) {
        if (it) {
            uri?.let { videoUri ->
                saveAvatar(videoUri)
                setVideoAvatar(videoUri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setListeners()
        if (!checkCameraPermission()) requestCameraPermission.launch(android.Manifest.permission.CAMERA)
        if (getIsVideo()) {
            setVideoAvatar(getAvatar())
        } else {
            setImageAvatar(getAvatar())
        }
    }

    private fun checkCameraPermission(): Boolean {
        return checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun setListeners() {
        binding.container.setOnClickListener { showAvatarTypeAlertDialog() }
    }

    private fun showAvatarTypeAlertDialog() {
        val dialogClickButtonClickListener = DialogInterface.OnClickListener { dialog, which ->
            when (which) {
                Dialog.BUTTON_POSITIVE -> {
                    if (checkCameraPermission()) {
                        getNewFile(isVideo = false)?.let { uri ->
                            this.uri = uri
                            takePicture.launch(uri)
                        }
                    } else {
                        requestCameraPermission.launch(android.Manifest.permission.CAMERA)
                    }
                }
                Dialog.BUTTON_NEGATIVE -> {
                    if (checkCameraPermission()) {
                        getNewFile(isVideo = true)?.let { uri ->
                            this.uri = uri
                            takeVideo.launch(uri)
                        }
                    } else {
                        requestCameraPermission.launch(android.Manifest.permission.CAMERA)
                    }
                }
            }
        }
        AlertDialog.Builder(this).setTitle("Сделайте выбор")
            .setPositiveButton("Image", dialogClickButtonClickListener)
            .setNegativeButton("Video", dialogClickButtonClickListener)
            .create()
            .show()
    }

    private fun getNewFile(isVideo: Boolean): Uri {
        val file = if (isVideo) {
            File.createTempFile("video_${System.currentTimeMillis()}", ".mp4", filesDir).apply {
                createNewFile()
            }
        } else {
            File.createTempFile("image_${System.currentTimeMillis()}", ".png", filesDir).apply {
                createNewFile()
            }
        }
        return FileProvider.getUriForFile(
            this, "com.example.videoimage_avatar.fileprovider", file
        )
    }
}