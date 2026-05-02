package com.example.recipebytes

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class PowerReceiver : BroadcastReceiver() {
    //BROADCAST_NOTIFICATION for power connectivity
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_POWER_CONNECTED) {
            showNotification(context)
        }
    }

    private fun showNotification(context: Context) {
        val channelId = "power_channel"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Power Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("RecipeBytes 🍽️")
            .setContentText("Good time to explore recipes!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(3001, notification)
    }
}