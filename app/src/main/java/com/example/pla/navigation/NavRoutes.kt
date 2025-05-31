package com.example.pla.navigation

object NavRoutes {
    const val CALENDAR = "calendar"
    const val ADD_NOTE = "add_note/{date}"
    const val EDIT_NOTE = "edit_note/{noteId}"
    
    fun addNote(date: String) = "add_note/$date"
    fun editNote(noteId: Long) = "edit_note/$noteId"
} 