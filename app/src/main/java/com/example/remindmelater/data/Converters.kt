package com.example.remindmelater.data

import androidx.room.TypeConverter
import com.example.remindmelater.data.model.ReminderStatus
import com.example.remindmelater.data.model.Timeframe

class Converters {
    @TypeConverter
    fun fromTimeframe(value: Timeframe): String = value.name

    @TypeConverter
    fun toTimeframe(value: String): Timeframe = Timeframe.valueOf(value)

    @TypeConverter
    fun fromStatus(value: ReminderStatus): String = value.name

    @TypeConverter
    fun toStatus(value: String): ReminderStatus = ReminderStatus.valueOf(value)
}
