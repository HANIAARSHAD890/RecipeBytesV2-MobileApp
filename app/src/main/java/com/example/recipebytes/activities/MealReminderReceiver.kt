package com.example.recipebytes

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.recipebytes.models.MealRepository
import java.util.Calendar

class MealReminderReceiver : BroadcastReceiver() {
    //custom notification for the MealReminder on a specific Days

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.example.recipebytes.MEAL_REMINDER") {

            // Load meals from SharedPreferences
            MealRepository.init(context)

            // Get today's day name
            val calendar = Calendar.getInstance()
            val dayName = when (calendar.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "Monday"
                Calendar.TUESDAY -> "Tuesday"
                Calendar.WEDNESDAY -> "Wednesday"
                Calendar.THURSDAY -> "Thursday"
                Calendar.FRIDAY -> "Friday"
                Calendar.SATURDAY -> "Saturday"
                Calendar.SUNDAY -> "Sunday"
                else -> ""
            }

            // Get meals for today
            val meals = MealRepository.getMealsForDay(dayName)

            // Show notification only if meals exist
            if (meals.isNotEmpty()) {
                showNotification(context, dayName, meals)
            }

            // Reschedule next alarm after 1 minute
            scheduleNext(context)
        }
    }

    private fun scheduleNext(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val nextIntent = Intent("com.example.recipebytes.MEAL_REMINDER")
        nextIntent.setClass(context, MealReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerTime = System.currentTimeMillis() + 60 * 1000L
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    private fun showNotification(context: Context, dayName: String, meals: List<String>) {
        val channelId = "meal_reminder_channel"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Meal Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val mealsText = meals.joinToString(", ")
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("$dayName Meal Plan 📅")
            .setContentText("Your meals: $mealsText")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Your meals: $mealsText"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(4001, notification)
    }
}