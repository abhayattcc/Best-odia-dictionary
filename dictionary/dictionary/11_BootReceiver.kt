package com.abhayattcc.dictionaryreader.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.abhayattcc.dictionaryreader.utils.FileUtils

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            FileUtils.checkAndRequestPermissions(context as Activity) {
                context.startService(Intent(context, ClipboardService::class.java))
            }
        }
    }
}