package com.example.remindmelater.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.remindmelater.data.ReminderDatabase
import com.example.remindmelater.data.SettingsDataStore
import com.example.remindmelater.data.model.Reminder
import com.example.remindmelater.data.model.ReminderStatus
import com.example.remindmelater.data.model.Timeframe
import com.example.remindmelater.scheduler.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DONE              = "com.example.remindmelater.ACTION_DONE"
        const val ACTION_SNOOZE_LATER_TODAY = "com.example.remindmelater.ACTION_SNOOZE_LATER_TODAY"
        const val ACTION_SNOOZE_TOMORROW   = "com.example.remindmelater.ACTION_SNOOZE_TOMORROW"
        const val ACTION_SNOOZE_RERANDOMIZE = "com.example.remindmelater.ACTION_SNOOZE_RERANDOMIZE"
        const val ACTION_RESCHEDULE        = "com.example.remindmelater.ACTION_RESCHEDULE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(ReminderScheduler.EXTRA_REMINDER_ID, -1L)
        if (reminderId == -1L) return

        val pendingResult = goAsync()
        val db = ReminderDatabase.getInstance(context)
        val settings = SettingsDataStore(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reminder = db.reminderDao().getById(reminderId) ?: return@launch
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                when (intent.action) {
                    ACTION_DONE -> {
                        db.reminderDao().updateStatus(reminderId, ReminderStatus.DONE)
                        ReminderScheduler.cancel(context, reminderId)
                        nm.cancel(reminderId.toInt())
                    }

                    ACTION_SNOOZE_LATER_TODAY -> {
                        snooze(context, db, settings, reminder, Timeframe.LATER_TODAY)
                        nm.cancel(reminderId.toInt())
                    }

                    ACTION_SNOOZE_TOMORROW -> {
                        snooze(context, db, settings, reminder, Timeframe.NEXT_FEW_DAYS)
                        nm.cancel(reminderId.toInt())
                    }

                    ACTION_SNOOZE_RERANDOMIZE -> {
                        snooze(context, db, settings, reminder, reminder.timeframe)
                        nm.cancel(reminderId.toInt())
                    }

                    ACTION_RESCHEDULE -> {
                        // Handled by launching MainActivity — just dismiss notification
                        nm.cancel(reminderId.toInt())
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun snooze(
        context: Context,
        db: ReminderDatabase,
        settings: SettingsDataStore,
        reminder: Reminder,
        newTimeframe: Timeframe
    ) {
        ReminderScheduler.cancel(context, reminder.id)
        val comfortStart = settings.comfortStart.first()
        val comfortEnd   = settings.comfortEnd.first()
        val newTime = ReminderScheduler.computeRandomTime(newTimeframe, comfortStart, comfortEnd)
        val updated = reminder.copy(
            timeframe = newTimeframe,
            scheduledAt = newTime,
            status = ReminderStatus.PENDING
        )
        db.reminderDao().update(updated)
        ReminderScheduler.schedule(context, updated)
    }
}
