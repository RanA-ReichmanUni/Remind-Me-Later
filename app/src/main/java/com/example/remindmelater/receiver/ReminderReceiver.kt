package com.example.remindmelater.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.remindmelater.R
import com.example.remindmelater.data.ReminderDatabase
import com.example.remindmelater.scheduler.ReminderScheduler
import com.example.remindmelater.ui.screens.AlarmActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "reminders"
        const val CHANNEL_NAME = "Reminders"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(ReminderScheduler.EXTRA_REMINDER_ID, -1L)
        if (reminderId == -1L) return

        val db = ReminderDatabase.getInstance(context)
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reminder = db.reminderDao().getById(reminderId) ?: return@launch
                showAlarmNotification(context, reminder.id, reminder.text)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showAlarmNotification(context: Context, reminderId: Long, text: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Ensure channel exists
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Dump & Forget reminders"
                enableVibration(true)
            }
            nm.createNotificationChannel(channel)
        }

        // Full-screen intent → AlarmActivity
        val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(ReminderScheduler.EXTRA_REMINDER_ID, reminderId)
            putExtra(AlarmActivity.EXTRA_REMINDER_TEXT, text)
        }
        val fullScreenPi = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Done action
        val doneIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_DONE
            putExtra(ReminderScheduler.EXTRA_REMINDER_ID, reminderId)
        }
        val donePi = PendingIntent.getBroadcast(
            context,
            (reminderId * 10 + 1).toInt(),
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Reminder")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPi, true)
            .setAutoCancel(true)
            .addAction(0, "✓ Done", donePi)
            .setContentIntent(fullScreenPi)
            .build()

        nm.notify(reminderId.toInt(), notification)
    }
}
