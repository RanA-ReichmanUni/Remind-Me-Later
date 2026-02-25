package com.example.remindmelater.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.remindmelater.data.model.Reminder
import com.example.remindmelater.data.model.Timeframe
import com.example.remindmelater.receiver.ReminderReceiver
import java.util.Calendar
import kotlin.random.Random

object ReminderScheduler {

    const val EXTRA_REMINDER_ID = "reminder_id"

    /**
     * Computes a random fire-time (epoch ms) within the given timeframe,
     * clamped to [comfortStart, comfortEnd) hours.
     *
     * LATER_TODAY: random minute in the remaining comfort window today.
     *   If fewer than 30 min remain, shifts to tomorrow morning.
     * Others: random day in [minDays, maxDays], random hour in comfort range.
     */
    fun computeRandomTime(
        timeframe: Timeframe,
        comfortStart: Int,
        comfortEnd: Int
    ): Long {
        val now = Calendar.getInstance()
        val cal = Calendar.getInstance()

        return when (timeframe) {
            Timeframe.LATER_TODAY -> {
                val startMinutes = maxOf(
                    now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE) + 5,
                    comfortStart * 60
                )
                val endMinutes = comfortEnd * 60

                if (startMinutes + 30 >= endMinutes) {
                    // Too late in the day — schedule for tomorrow in comfort hours
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                    val randomMinute = comfortStart * 60 + Random.nextInt((comfortEnd - comfortStart) * 60)
                    cal.set(Calendar.HOUR_OF_DAY, randomMinute / 60)
                    cal.set(Calendar.MINUTE, randomMinute % 60)
                } else {
                    val randomMinute = startMinutes + Random.nextInt(endMinutes - startMinutes)
                    cal.set(Calendar.HOUR_OF_DAY, randomMinute / 60)
                    cal.set(Calendar.MINUTE, randomMinute % 60)
                }
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }

            else -> {
                val daysToAdd = timeframe.minDays + Random.nextInt(timeframe.maxDays - timeframe.minDays + 1)
                cal.add(Calendar.DAY_OF_YEAR, daysToAdd)
                val randomHour = comfortStart + Random.nextInt(comfortEnd - comfortStart)
                val randomMinute = Random.nextInt(60)
                cal.set(Calendar.HOUR_OF_DAY, randomHour)
                cal.set(Calendar.MINUTE, randomMinute)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
        }
    }

    fun schedule(context: Context, reminder: Reminder) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_REMINDER_ID, reminder.id)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.scheduledAt, pi)
    }

    fun cancel(context: Context, reminderId: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pi?.let { am.cancel(it) }
    }

    fun rescheduleAll(context: Context, reminders: List<Reminder>) {
        reminders.forEach { schedule(context, it) }
    }
}
