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
import com.example.recipebytes.activities.MainActivity
import com.example.recipebytes.models.MealFirebaseRepository
import java.util.Calendar

class MealReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.example.recipebytes.MEAL_REMINDER") {

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

            val monthKey = "%d-%02d".format(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1
            )

            val todayDate = "%d-%02d-%02d".format(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            MealFirebaseRepository.loadMonthMeals(
                monthKey,
                onSuccess = { savedDays ->
                    val mealDay = savedDays.find { it.date == todayDate }

                    val hasAny = mealDay?.let {
                        it.breakfast.isNotEmpty() || it.lunch.isNotEmpty() ||
                                it.dinner.isNotEmpty() || it.dessert.isNotEmpty()
                    } ?: false

                    if (hasAny && mealDay != null) {
                        showCategoryNotification(context, dayName, todayDate, mealDay)
                    }

                    scheduleNext(context)
                },
                onError = {
                    scheduleNext(context)
                }
            )
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

    private fun showCategoryNotification(
        context: Context,
        dayName: String,
        todayDate: String,
        mealDay: com.example.recipebytes.models.MealDay
    ) {
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

        // Format date e.g. "7 June 2026"
        val parts = todayDate.split("-")
        val formattedDate = if (parts.size == 3)
            "${parts[2].trimStart('0')} ${monthFullName(parts[1].toInt())} ${parts[0]}"
        else todayDate

        // Build category wise message
        val sb = StringBuilder()
        if (mealDay.breakfast.isNotEmpty())
            sb.append("🌅 Breakfast: ${mealDay.breakfast.joinToString(", ")}\n")
        if (mealDay.lunch.isNotEmpty())
            sb.append("☀️ Lunch: ${mealDay.lunch.joinToString(", ")}\n")
        if (mealDay.dinner.isNotEmpty())
            sb.append("🌙 Dinner: ${mealDay.dinner.joinToString(", ")}\n")
        if (mealDay.dessert.isNotEmpty())
            sb.append("🍰 Dessert: ${mealDay.dessert.joinToString(", ")}")

        val fullText = sb.toString().trim()

        val totalMeals = mealDay.breakfast.size + mealDay.lunch.size +
                mealDay.dinner.size + mealDay.dessert.size
        val shortText = "$totalMeals meal${if (totalMeals > 1) "s" else ""} planned today"

        // Navigate to planner screen on click
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("open_planner", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context, 1001, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("📅 $dayName, $formattedDate")
            .setContentText(shortText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(fullText)
                    .setBigContentTitle("📅 $dayName, $formattedDate")
                    .setSummaryText("Tap to open Meal Planner")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(tapPendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(4001, notification)
    }

    private fun monthFullName(month: Int) = arrayOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )[month - 1]
}