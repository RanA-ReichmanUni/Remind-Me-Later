package com.example.remindmelater.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.remindmelater.data.ReminderDatabase
import com.example.remindmelater.data.ReminderRepository
import com.example.remindmelater.data.SettingsDataStore
import com.example.remindmelater.data.model.Reminder
import com.example.remindmelater.data.model.ReminderStatus
import com.example.remindmelater.data.model.Timeframe
import com.example.remindmelater.scheduler.ReminderScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ReminderViewModel(app: Application) : AndroidViewModel(app) {

    private val db         = ReminderDatabase.getInstance(app)
    private val repository = ReminderRepository(db.reminderDao())
    private val settings   = SettingsDataStore(app)

    // Public state
    val reminders: StateFlow<List<Reminder>> = repository
        .getActiveReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val comfortStart: StateFlow<Int> = settings.comfortStart
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 9)

    val comfortEnd: StateFlow<Int> = settings.comfortEnd
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 21)

    val hasOnboarded: StateFlow<Boolean> = settings.hasOnboarded
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    // -------------------------------------------------------------------------
    // Reminder operations
    // -------------------------------------------------------------------------

    fun addReminder(text: String, timeframe: Timeframe, ignoreComfortHours: Boolean = false) {
        viewModelScope.launch {
            val scheduledAt = ReminderScheduler.computeRandomTime(
                timeframe,
                comfortStart.value,
                comfortEnd.value,
                ignoreComfortHours
            )
            val reminder = Reminder(
                text       = text.trim(),
                timeframe  = timeframe,
                scheduledAt = scheduledAt
            )
            val id = repository.insert(reminder)
            ReminderScheduler.schedule(getApplication(), reminder.copy(id = id))
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            ReminderScheduler.cancel(getApplication(), reminder.id)
            repository.delete(reminder)
        }
    }

    fun updateTimeframe(reminder: Reminder, newTimeframe: Timeframe) {
        viewModelScope.launch {
            ReminderScheduler.cancel(getApplication(), reminder.id)
            val newTime = ReminderScheduler.computeRandomTime(
                newTimeframe,
                comfortStart.value,
                comfortEnd.value
            )
            val updated = reminder.copy(
                timeframe   = newTimeframe,
                scheduledAt = newTime,
                status      = ReminderStatus.PENDING
            )
            repository.update(updated)
            ReminderScheduler.schedule(getApplication(), updated)
        }
    }

    fun markDone(reminderId: Long) {
        viewModelScope.launch {
            ReminderScheduler.cancel(getApplication(), reminderId)
            repository.markDone(reminderId)
        }
    }

    // -------------------------------------------------------------------------
    // Settings
    // -------------------------------------------------------------------------

    fun saveComfortHours(start: Int, end: Int) {
        viewModelScope.launch {
            settings.saveComfortHours(start, end)
        }
    }
}
