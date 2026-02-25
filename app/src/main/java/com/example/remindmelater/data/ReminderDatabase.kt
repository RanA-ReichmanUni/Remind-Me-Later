package com.example.remindmelater.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.remindmelater.data.model.Reminder

@Database(entities = [Reminder::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class ReminderDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: ReminderDatabase? = null

        fun getInstance(context: Context): ReminderDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ReminderDatabase::class.java,
                    "reminders.db"
                ).build().also { INSTANCE = it }
            }
    }
}
