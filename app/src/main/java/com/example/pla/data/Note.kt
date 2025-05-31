package com.example.pla.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val color: Int // Будем хранить цвет как Int
)

class Converters {
    @TypeConverter
    fun fromLocalDate(value: String): LocalDate = LocalDate.parse(value)

    @TypeConverter
    fun localDateToString(date: LocalDate): String = date.toString()

    @TypeConverter
    fun fromLocalTime(value: String): LocalTime = LocalTime.parse(value)

    @TypeConverter
    fun localTimeToString(time: LocalTime): String = time.toString()
} 