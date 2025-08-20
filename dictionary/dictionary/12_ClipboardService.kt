package com.abhayattcc.dictionaryreader.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.abhayattcc.dictionaryreader.App
import com.abhayattcc.dictionaryreader.R
import com.abhayattcc.dictionaryreader.activities.MainActivity
import com.abhayattcc.dictionaryreader.databinding.PopupDictionaryBinding
import com.abhayattcc.dictionaryreader.utils.TtsUtils
import com.abhayattcc.dictionaryreader.viewmodels.DictionaryViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ClipboardService : Service() {
    private lateinit var viewModel: DictionaryViewModel

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, buildNotification())
        viewModel = DictionaryViewModel(application, (application as App).database.dictionaryDao())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.addPrimaryClipChangedListener {
            val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
            if (text != null) {
                CoroutineScope(Dispatchers.Main).launch {
                    showPopup(text)
                }
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "clipboard_channel",
                "Clipboard Monitoring",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, "clipboard_channel")
            .setContentTitle("Dictionary Reader")
            .setContentText("Monitoring clipboard for text")
            .setSmallIcon(R.drawable.ic_app_icon)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun showPopup(text: String) {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            android.graphics.PixelFormat.TRANSLUCENT
        )
        layoutParams.gravity = android.view.Gravity.CENTER
        val binding = PopupDictionaryBinding.inflate(LayoutInflater.from(this))
        binding.popupClose.setOnClickListener {
            windowManager.removeView(binding.root)
        }
        TtsUtils.initialize(this) { voices ->
            val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, voices.map { "${it.name} (${it.locale.language})" })
            binding.popupVoiceSelect1.adapter = spinnerAdapter
            binding.popupVoiceSelect2.adapter = spinnerAdapter
            binding.popupVoiceSelect3.adapter = spinnerAdapter
        }
        binding.popupSpeak.setOnClickListener {
            TtsUtils.speak(binding.popupContent.text.toString(), binding.popupVoiceSelect1.selectedItem.toString().substringAfter("(").substringBefore(")")) {
                binding.popupSpeak.isEnabled = true
                binding.popupStop.isEnabled = false
            }
            binding.popupSpeak.isEnabled = false
            binding.popupStop.isEnabled = true
        }
        binding.popupStop.setOnClickListener {
            TtsUtils.stop()
            binding.popupSpeak.isEnabled = true
            binding.popupStop.isEnabled = false
        }
        windowManager.addView(binding.root, layoutParams)
        CoroutineScope(Dispatchers.Main).launch {
            viewModel.searchWord(text, true)
            viewModel.results.observeForever {
                binding.popupContent.text = it
            }
        }
    }
}