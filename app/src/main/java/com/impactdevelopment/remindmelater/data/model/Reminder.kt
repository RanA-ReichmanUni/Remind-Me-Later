package com.impactdevelopment.remindmelater.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Timeframe(val label: String, val minDays: Int, val maxDays: Int) {
    LATER_TODAY("Later today", 0, 0),
    NEXT_FEW_DAYS("Next few days", 1, 4),
    NEXT_WEEKS("Next weeks", 7, 21),
    NEXT_MONTH("Next month", 22, 45)
}

enum class ReminderStatus {
    PENDING, DONE, SNOOZED
}

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val text: String,
    val timeframe: Timeframe,
    val scheduledAt: Long,      // epoch ms — internal only, never shown to user
    val status: ReminderStatus = ReminderStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis()
)
