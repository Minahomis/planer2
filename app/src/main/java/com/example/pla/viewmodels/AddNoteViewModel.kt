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
}

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