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

        // Поиск паттерна "с X до Y"
        val rangePattern = "с (\\d{1,2}) до (\\d{1,2})".toRegex()
        rangePattern.find(text)?.let { match ->
            val (start, end) = match.destructured
            startTime = LocalTime.of(start.toInt(), 0)
            endTime = LocalTime.of(end.toInt(), 0)
        }

        // Если не найден паттерн "с X до Y", ищем "в X"
        if (startTime == null) {
            val singlePattern = "в (\\d{1,2})".toRegex()
            singlePattern.find(text)?.let { match ->
                val (hour) = match.destructured
                startTime = LocalTime.of(hour.toInt(), 0)
                endTime = startTime!!.plusHours(1)
            }
        }

        // Поиск цвета
        val colorPattern = "цвет (\\w+)".toRegex()
        colorPattern.find(text)?.let { match ->
            val (colorName) = match.destructured
            color = when (colorName.lowercase()) {
                "красный" -> Color(0xFFDB4437)
                "зеленый", "зелёный" -> Color(0xFF0F9D58)
                "желтый", "жёлтый" -> Color(0xFFF4B400)
                "фиолетовый" -> Color(0xFF7B1FA2)
                else -> Color(0xFF1A73E8) // синий по умолчанию
            }
        }

        // Если время не указано, используем текущее время
        if (startTime == null) {
            startTime = LocalTime.now()
            endTime = startTime!!.plusHours(1)
        }

        return QuickNoteData(text, startTime!!, endTime!!, color)
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