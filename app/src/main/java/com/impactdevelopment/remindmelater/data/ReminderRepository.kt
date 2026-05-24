package com.impactdevelopment.remindmelater.data

import com.impactdevelopment.remindmelater.data.model.Reminder
import com.impactdevelopment.remindmelater.data.model.ReminderStatus
import com.impactdevelopment.remindmelater.data.model.Timeframe
import kotlinx.coroutines.flow.Flow

class ReminderRepository(private val dao: ReminderDao) {

    fun getActiveReminders(): Flow<List<Reminder>> = dao.getActiveReminders()

    suspend fun getActiveRemindersList(): List<Reminder> = dao.getActiveRemindersList()

    suspend fun getById(id: Long): Reminder? = dao.getById(id)

    /** Inserts a reminder and returns the auto-generated id. */
    suspend fun insert(reminder: Reminder): Long = dao.insert(reminder)

    suspend fun update(reminder: Reminder) = dao.update(reminder)

    suspend fun delete(reminder: Reminder) = dao.delete(reminder)

    suspend fun markDone(id: Long) = dao.updateStatus(id, ReminderStatus.DONE)

    suspend fun markSnoozed(id: Long) = dao.updateStatus(id, ReminderStatus.SNOOZED)
}
