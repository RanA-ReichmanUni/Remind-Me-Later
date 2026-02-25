package com.example.remindmelater.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.remindmelater.data.ReminderDatabase
import com.example.remindmelater.scheduler.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        val pendingResult = goAsync()
        val db = ReminderDatabase.getInstance(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val pending = db.reminderDao().getActiveRemindersList()
                ReminderScheduler.rescheduleAll(context, pending)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
