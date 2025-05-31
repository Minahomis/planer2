package com.example.pla.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE startDate >= :startDate AND endDate <= :endDate")
    fun getNotesForPeriod(startDate: LocalDate, endDate: LocalDate): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :noteId")
    fun getNoteById(noteId: Long): Flow<NoteEntity>

    @Insert
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity)

    @Delete
    suspend fun delete(note: NoteEntity)
} 