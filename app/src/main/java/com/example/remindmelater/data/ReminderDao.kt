package com.example.remindmelater.data

import androidx.room.*
import com.example.remindmelater.data.model.Reminder
import com.example.remindmelater.data.model.ReminderStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE status = 'PENDING' OR status = 'SNOOZED' ORDER BY scheduledAt ASC")
    fun getActiveReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders ORDER BY scheduledAt ASC")
    fun getAllReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE status = 'PENDING' OR status = 'SNOOZED'")
    suspend fun getActiveRemindersList(): List<Reminder>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: Long): Reminder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: Reminder): Long

    @Update
    suspend fun update(reminder: Reminder)

    @Delete
    suspend fun delete(reminder: Reminder)

    @Query("UPDATE reminders SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: ReminderStatus)
}
