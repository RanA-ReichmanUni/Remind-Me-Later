package com.impactdevelopment.remindmelater.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.impactdevelopment.remindmelater.data.ReminderDatabase
import com.impactdevelopment.remindmelater.data.ReminderRepository
import com.impactdevelopment.remindmelater.data.SettingsDataStore
import com.impactdevelopment.remindmelater.data.model.Reminder
import com.impactdevelopment.remindmelater.data.model.ReminderStatus
import com.impactdevelopment.remindmelater.data.model.Timeframe
import com.impactdevelopment.remindmelater.scheduler.ReminderScheduler
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

    val termsAccepted: StateFlow<Boolean> = settings.termsAccepted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val backgroundAnimationsEnabled: StateFlow<Boolean> = settings.backgroundAnimationsEnabled
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

    /** TODO: REMOVE BEFORE RELEASE — fires reminder in exactly 1 minute */
    fun scheduleInOneMinute(text: String) {
        viewModelScope.launch {
            val scheduledAt = System.currentTimeMillis() + 60_000L
            val reminder = Reminder(
                text        = text.trim().ifBlank { "Test reminder" },
                timeframe   = Timeframe.LATER_TODAY,
                scheduledAt = scheduledAt
            )
            val id = repository.insert(reminder)
            ReminderScheduler.schedule(getApplication(), reminder.copy(id = id))
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

    fun acceptTerms() {
        viewModelScope.launch {
            settings.setTermsAccepted(true)
        }
    }

    fun setBackgroundAnimationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settings.setBackgroundAnimationsEnabled(enabled)
        }
    }
}
