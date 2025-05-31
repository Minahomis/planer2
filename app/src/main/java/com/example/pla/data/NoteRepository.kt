package com.example.pla.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class NoteRepository(private val noteDao: NoteDao) {
    fun getNotesForPeriod(startDate: LocalDate, endDate: LocalDate): Flow<List<NoteEntity>> {
        return noteDao.getNotesForPeriod(startDate, endDate)
    }

    fun getAllNotes(): Flow<List<NoteEntity>> {
        return noteDao.getAllNotes()
    }

    fun getNoteById(noteId: Long): Flow<NoteEntity> {
        return noteDao.getNoteById(noteId)
    }

    suspend fun insert(note: NoteEntity): Long {
        return noteDao.insert(note)
    }

    suspend fun update(note: NoteEntity) {
        noteDao.update(note)
    }

    suspend fun delete(note: NoteEntity) {
        noteDao.delete(note)
    }
} 