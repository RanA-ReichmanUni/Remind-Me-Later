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
import java.util.Calendar
import kotlin.random.Random

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DONE               = "com.example.remindmelater.ACTION_DONE"
        const val ACTION_SNOOZE_LATER_TODAY  = "com.example.remindmelater.ACTION_SNOOZE_LATER_TODAY"
        const val ACTION_SNOOZE_TOMORROW     = "com.example.remindmelater.ACTION_SNOOZE_TOMORROW"
        const val ACTION_SNOOZE_NEXT_FEW_DAYS = "com.example.remindmelater.ACTION_SNOOZE_NEXT_FEW_DAYS"
        const val ACTION_SNOOZE_NEXT_WEEKS   = "com.example.remindmelater.ACTION_SNOOZE_NEXT_WEEKS"
        const val ACTION_SNOOZE_NEXT_MONTH   = "com.example.remindmelater.ACTION_SNOOZE_NEXT_MONTH"
        const val ACTION_RESCHEDULE          = "com.example.remindmelater.ACTION_RESCHEDULE"
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
                        snoozeTomorrow(context, db, settings, reminder)
                        nm.cancel(reminderId.toInt())
                    }

                    ACTION_SNOOZE_NEXT_FEW_DAYS -> {
                        snooze(context, db, settings, reminder, Timeframe.NEXT_FEW_DAYS)
                        nm.cancel(reminderId.toInt())
                    }

                    ACTION_SNOOZE_NEXT_WEEKS -> {
                        snooze(context, db, settings, reminder, Timeframe.NEXT_WEEKS)
                        nm.cancel(reminderId.toInt())
                    }

                    ACTION_SNOOZE_NEXT_MONTH -> {
                        snooze(context, db, settings, reminder, Timeframe.NEXT_MONTH)
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

    /** Schedules exactly tomorrow at a random hour within comfort window. */
    private suspend fun snoozeTomorrow(
        context: Context,
        db: ReminderDatabase,
        settings: SettingsDataStore,
        reminder: Reminder
    ) {
        ReminderScheduler.cancel(context, reminder.id)
        val comfortStart = settings.comfortStart.first()
        val comfortEnd   = settings.comfortEnd.first()
        val hour = if (comfortEnd > comfortStart) {
            comfortStart + Random.nextInt(comfortEnd - comfortStart)
        } else {
            (comfortStart + Random.nextInt((24 - comfortStart) + comfortEnd)) % 24
        }
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, Random.nextInt(60))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val updated = reminder.copy(
            timeframe   = Timeframe.NEXT_FEW_DAYS,
            scheduledAt = cal.timeInMillis,
            status      = ReminderStatus.PENDING
        )
        db.reminderDao().update(updated)
        ReminderScheduler.schedule(context, updated)
    }
}
