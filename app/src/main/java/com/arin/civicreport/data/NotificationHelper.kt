package com.arin.civicreport.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.arin.civicreport.R

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "report_updates"
        const val NOTIFICATION_ID = 4201
    }

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Report updates",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Confirmation when a complaint is handed to your mail or SMS app" }
        manager.createNotificationChannel(channel)
    }

    fun canNotify(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    fun notifyReportSent(title: String, authorityCount: Int) {
        if (!canNotify()) return
        ensureChannel()
        val text = if (authorityCount > 0) {
            "Complaint prepared for $authorityCount office(s). Confirm sending in your mail or SMS app."
        } else {
            "Complaint prepared. Confirm sending in your mail or SMS app."
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_report)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification) }
    }
}
