package com.example.pla.viewmodels

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pla.data.NoteEntity
import com.example.pla.data.NoteRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

class AddNoteViewModel(
    private val repository: NoteRepository,
    private val noteId: Long? = null
) : ViewModel() {

    private val _noteState = MutableStateFlow<NoteEntity?>(null)
    val noteState: StateFlow<NoteEntity?> = _noteState.asStateFlow()

    init {
        if (noteId != null) {
            loadNote(noteId)
        }
    }

    private fun loadNote(id: Long) {
        viewModelScope.launch {
            repository.getNoteById(id).collect { note ->
                _noteState.value = note
            }
        }
    }

    fun saveQuickNote(text: String, date: LocalDate) {
        viewModelScope.launch {
            val (title, startTime, endTime, color) = parseQuickNote(text)
            val note = NoteEntity(
                id = 0,
                title = title,
                startDate = date,
                endDate = date,
                startTime = startTime,
                endTime = endTime,
                color = color.toArgb()
            )
            repository.insert(note)
        }
    }

    private fun parseQuickNote(text: String): QuickNoteData {
        var startTime: LocalTime? = null
        var endTime: LocalTime? = null
        var color = Color(0xFF1A73E8) // Default blue color
        val lowerText = text.lowercase().replace('ё', 'е')

        // Паттерны точного времени
        val exactTimePattern = "(\\d{1,2})[:\\.-](\\d{2})".toRegex()
        
        // Паттерны диапазонов
        val rangeExactPattern = "с\\s*(\\d{1,2})[:\\.-](\\d{2})\\s*до\\s*(\\d{1,2})[:\\.-](\\d{2})".toRegex()
        val rangeSimplePattern = "с\\s*(\\d{1,2})\\s*до\\s*(\\d{1,2})".toRegex()
        
        // Паттерны для точного времени с предлогом
        val inExactTimePattern = "в\\s*(\\d{1,2})[:\\.-](\\d{2})".toRegex()
        val inHourPattern = "в\\s*(\\d{1,2})\\s*(час|часов|ч)?".toRegex()
        
        // Паттерны времени суток
        val morningPattern = "утра|утром|с утра".toRegex()
        val dayPattern = "дня|днем|после полудня".toRegex()
        val eveningPattern = "вечера|вечером|веч".toRegex()
        val nightPattern = "ночи|ночью".toRegex()
        
        // Паттерны для частей часа
        val halfHourPattern = "(пол|половина|половину)\\s*(\\d{1,2})(го|ого|го часа)?".toRegex()
        val quarterHourPattern = "(четверть|15 минут)\\s*(\\d{1,2})(го|ого|го часа)?".toRegex()
        
        // Дополнительные временные паттерны
        val shortTimePattern = "в\\s*(\\d{1,2})(:\\d{2})?\\s*(вечера|утра|дня|ночи)?".toRegex()
        val naturalTimePattern = "(через\\s*|после\\s*)(пол|половину|четверть)\\s*(\\d{1,2})".toRegex()
        val minutesPattern = "(\\d{1,2})\\s*минут\\s*(\\d{1,2})(го)?".toRegex()

        // Проверяем паттерны в порядке приоритета
        when {
            // "с 15:00 до 16:30" или "с 15.00 до 16.30"
            rangeExactPattern.find(lowerText)?.let { match ->
                val (startHour, startMinute, endHour, endMinute) = match.destructured
                startTime = LocalTime.of(startHour.toInt(), startMinute.toInt())
                endTime = LocalTime.of(endHour.toInt(), endMinute.toInt())
            } != null -> {}

            // "с 15 до 16"
            rangeSimplePattern.find(lowerText)?.let { match ->
                val (start, end) = match.destructured
                startTime = LocalTime.of(start.toInt(), 0)
                endTime = LocalTime.of(end.toInt(), 0)
            } != null -> {}

            // "20 минут 3-го" -> 02:20
            minutesPattern.find(lowerText)?.let { match ->
                val (minutes, hour) = match.destructured
                var adjustedHour = hour.toInt() - 1
                adjustedHour = adjustTimeOfDay(adjustedHour, lowerText)
                startTime = LocalTime.of(adjustedHour, minutes.toInt())
                endTime = startTime!!.plusHours(1)
            } != null -> {}

            // "в 15:30" или "в 15.30"
            inExactTimePattern.find(lowerText)?.let { match ->
                val (hour, minute) = match.destructured
                startTime = LocalTime.of(hour.toInt(), minute.toInt())
                endTime = startTime!!.plusHours(1)
            } != null -> {}

            // "в 3" или "в 3 вечера"
            shortTimePattern.find(lowerText)?.let { match ->
                val (hour, minutes, period) = match.destructured
                var adjustedHour = hour.toInt()
                
                // Определяем время суток по контексту
                adjustedHour = when (period) {
                    "утра" -> adjustedHour
                    "дня", "вечера" -> if (adjustedHour < 12) adjustedHour + 12 else adjustedHour
                    "ночи" -> if (adjustedHour == 12) 0 else if (adjustedHour < 7) adjustedHour else adjustedHour + 12
                    else -> adjustTimeOfDay(adjustedHour, lowerText)
                }
                
                startTime = LocalTime.of(adjustedHour, minutes?.drop(1)?.toIntOrNull() ?: 0)
                endTime = startTime!!.plusHours(1)
            } != null -> {}

            // "через полвторого" -> 14:30
            naturalTimePattern.find(lowerText)?.let { match ->
                val (_, fraction, hour) = match.destructured
                var adjustedHour = hour.toInt()
                adjustedHour = adjustTimeOfDay(adjustedHour, lowerText)
                
                val minutes = when (fraction) {
                    "пол", "половину" -> 30
                    "четверть" -> 15
                    else -> 0
                }
                
                startTime = LocalTime.of(adjustedHour - 1, minutes)
                endTime = startTime!!.plusHours(1)
            } != null -> {}

            // "в пол второго" -> 13:30
            halfHourPattern.find(lowerText)?.let { match ->
                val (_, hour) = match.destructured
                var adjustedHour = hour.toInt()
                adjustedHour = adjustTimeOfDay(adjustedHour, lowerText)
                startTime = LocalTime.of(adjustedHour - 1, 30)
                endTime = startTime!!.plusHours(1)
            } != null -> {}

            // "в четверть третьего" -> 14:15
            quarterHourPattern.find(lowerText)?.let { match ->
                val (_, hour) = match.destructured
                var adjustedHour = hour.toInt()
                adjustedHour = adjustTimeOfDay(adjustedHour, lowerText)
                startTime = LocalTime.of(adjustedHour - 1, 15)
                endTime = startTime!!.plusHours(1)
            } != null -> {}

            // Простое указание времени без "в" (например, "15:00")
            exactTimePattern.find(lowerText)?.let { match ->
                val (hour, minute) = match.destructured
                startTime = LocalTime.of(hour.toInt(), minute.toInt())
                endTime = startTime!!.plusHours(1)
            } != null -> {}

            else -> {
                startTime = LocalTime.now()
                endTime = startTime!!.plusHours(1)
            }
        }

        // Поиск цвета
        val colorPattern = "цвет\\s*(\\w+)".toRegex()
        colorPattern.find(lowerText)?.let { match ->
            val (colorName) = match.destructured
            color = when (colorName) {
                "красный" -> Color(0xFFDB4437)
                "зеленый", "зеленый" -> Color(0xFF0F9D58)
                "желтый", "желтый" -> Color(0xFFF4B400)
                "фиолетовый" -> Color(0xFF7B1FA2)
                "синий" -> Color(0xFF1A73E8)
                else -> Color(0xFF1A73E8)
            }
        }

        return QuickNoteData(text, startTime!!, endTime!!, color)
    }

    private fun adjustTimeOfDay(hour: Int, text: String): Int {
        return when {
            text.contains(Regex("утра|утром|с утра")) -> hour
            text.contains(Regex("дня|днем|после полудня")) -> if (hour < 12) hour + 12 else hour
            text.contains(Regex("вечера|вечером|веч")) -> if (hour < 12) hour + 12 else hour
            text.contains(Regex("ночи|ночью")) -> if (hour == 12) 0 else if (hour < 7) hour else hour + 12
            hour <= 5 -> hour + 12 // По умолчанию считаем вечером
            else -> hour
        }
    }

    fun saveNote(
        title: String,
        startDate: LocalDate,
        endDate: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        color: Color
    ) {
        viewModelScope.launch {
            val note = NoteEntity(
                id = noteId ?: 0,
                title = title,
                startDate = startDate,
                endDate = endDate,
                startTime = startTime,
                endTime = endTime,
                color = color.toArgb()
            )

            if (noteId != null) {
                repository.update(note)
            } else {
                repository.insert(note)
            }
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.delete(note)
        }
    }
}

private data class QuickNoteData(
    val title: String,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val color: Color
)

class AddNoteViewModelFactory(
    private val repository: NoteRepository,
    private val noteId: Long? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddNoteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddNoteViewModel(repository, noteId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 