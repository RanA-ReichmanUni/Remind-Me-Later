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
        comfortEnd: Int,
        ignoreComfortHours: Boolean = false
    ): Long {
        val now = Calendar.getInstance()
        val cal = Calendar.getInstance()

        return when (timeframe) {
            Timeframe.LATER_TODAY -> {
                val nowMin    = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
                val nightShift = comfortEnd <= comfortStart // window spans midnight

                if (ignoreComfortHours) {
                    val toMidnight = 24 * 60 - nowMin
                    if (!nightShift && toMidnight >= 30) {
                        // Daytime worker: fire 30 min–4 hrs from now, capped at 23:59
                        val floor = nowMin + 30
                        val ceil  = minOf(nowMin + 240, 23 * 60 + 59)
                        val fire  = if (floor < ceil) floor + Random.nextInt(ceil - floor) else floor
                        cal.set(Calendar.HOUR_OF_DAY, fire / 60)
                        cal.set(Calendar.MINUTE, fire % 60)
                    } else {
                        // Night-shift worker OR < 30 min to midnight:
                        // fire 30–60 min from now; Calendar auto-rolls to next day if needed
                        cal.add(Calendar.MINUTE, 30 + Random.nextInt(30))
                    }
                } else if (!nightShift) {
                    // Normal daytime comfort window
                    val startMin = maxOf(nowMin + 5, comfortStart * 60)
                    val endMin   = comfortEnd * 60
                    if (startMin + 30 >= endMin) {
                        cal.add(Calendar.DAY_OF_YEAR, 1)
                        val r = comfortStart * 60 + Random.nextInt((comfortEnd - comfortStart) * 60)
                        cal.set(Calendar.HOUR_OF_DAY, r / 60)
                        cal.set(Calendar.MINUTE, r % 60)
                    } else {
                        val r = startMin + Random.nextInt(endMin - startMin)
                        cal.set(Calendar.HOUR_OF_DAY, r / 60)
                        cal.set(Calendar.MINUTE, r % 60)
                    }
                } else {
                    // Night-shift: window = [comfortStart*60, 24*60) ∪ [0, comfortEnd*60)
                    val inWindow  = nowMin >= comfortStart * 60 || nowMin < comfortEnd * 60
                    val windowLen = (24 - comfortStart) * 60 + comfortEnd * 60
                    val nowLinear = if (nowMin >= comfortStart * 60)
                        nowMin - comfortStart * 60
                    else
                        (24 - comfortStart) * 60 + nowMin
                    val floorLinear = nowLinear + 5

                    if (!inWindow || floorLinear + 30 >= windowLen) {
                        // Outside window or near end — push to next window start
                        if (comfortStart * 60 > nowMin)
                            cal.set(Calendar.HOUR_OF_DAY, comfortStart)
                        else {
                            cal.add(Calendar.DAY_OF_YEAR, 1)
                            cal.set(Calendar.HOUR_OF_DAY, comfortStart)
                        }
                        cal.set(Calendar.MINUTE, Random.nextInt(60))
                    } else {
                        val r      = floorLinear + Random.nextInt(windowLen - floorLinear)
                        val absMin = (comfortStart * 60 + r) % (24 * 60)
                        if (absMin < nowMin) cal.add(Calendar.DAY_OF_YEAR, 1)
                        cal.set(Calendar.HOUR_OF_DAY, absMin / 60)
                        cal.set(Calendar.MINUTE, absMin % 60)
                    }
                }
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }

            else -> {
                val daysToAdd  = timeframe.minDays + Random.nextInt(timeframe.maxDays - timeframe.minDays + 1)
                cal.add(Calendar.DAY_OF_YEAR, daysToAdd)
                val nightShift = comfortEnd <= comfortStart
                val randomHour = if (!nightShift) {
                    comfortStart + Random.nextInt(comfortEnd - comfortStart)
                } else {
                    // Night-shift: pick from [comfortStart, 24) ∪ [0, comfortEnd)
                    (comfortStart + Random.nextInt((24 - comfortStart) + comfortEnd)) % 24
                }
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

        // setAlarmClock() is the ONLY AlarmManager method that grants the receiving
        // app a background-activity-start exemption on Android 10+. Without it,
        // context.startActivity() from a BroadcastReceiver is silently blocked and
        // only the notification heads-up shows. The "showIntent" is shown in the
        // system clock / status-bar; opening MainActivity is correct here.
        val showPi = PendingIntent.getActivity(
            context,
            (reminder.id + 100_000L).toInt(),
            Intent().apply {
                setClassName(context, "com.example.remindmelater.MainActivity")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        try {
            am.setAlarmClock(AlarmManager.AlarmClockInfo(reminder.scheduledAt, showPi), pi)
        } catch (e: SecurityException) {
            // Exact alarm permission not granted — fall back to inexact.
            // Full-screen launch won't work in this path, but the notification will.
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.scheduledAt, pi)
        }
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
