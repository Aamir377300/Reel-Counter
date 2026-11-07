package com.aamir.reelcounter.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromTrackedApp(value: TrackedApp): String = value.name

    @TypeConverter
    fun toTrackedApp(value: String): TrackedApp = TrackedApp.valueOf(value)
}
