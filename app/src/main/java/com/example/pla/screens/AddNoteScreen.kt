package com.example.pla.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.pla.components.TimePickerDialog
import com.example.pla.data.NoteDatabase
import com.example.pla.data.NoteRepository
import com.example.pla.viewmodels.AddNoteViewModel
import com.example.pla.viewmodels.AddNoteViewModelFactory
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNoteScreen(
    navController: NavController,
    selectedDate: LocalDate = LocalDate.now(),
    noteId: Long? = null
) {
    val context = LocalContext.current
    val database = NoteDatabase.getDatabase(context)
    val repository = NoteRepository(database.noteDao())
    val viewModel: AddNoteViewModel = viewModel(
        factory = AddNoteViewModelFactory(repository, noteId)
    )

    val existingNote by viewModel.noteState.collectAsState()

    var title by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(selectedDate) }
    var endDate by remember { mutableStateOf(selectedDate) }
    var startTime by remember { mutableStateOf(LocalTime.now()) }
    var endTime by remember { mutableStateOf(LocalTime.now().plusHours(1)) }
    var selectedColor by remember { mutableStateOf(Color(0xFF1A73E8)) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    // Загружаем данные существующей заметки
    LaunchedEffect(existingNote) {
        existingNote?.let { note ->
            title = note.title
            startDate = note.startDate
            endDate = note.endDate
            startTime = note.startTime
            endTime = note.endTime
            selectedColor = Color(note.color)
        }
    }

    val colors = listOf(
        Color(0xFF1A73E8), // Blue
        Color(0xFFDB4437), // Red
        Color(0xFF0F9D58), // Green
        Color(0xFFF4B400), // Yellow
        Color(0xFF7B1FA2)  // Purple
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.saveNote(
                                title = title,
                                startDate = startDate,
                                endDate = endDate,
                                startTime = startTime,
                                endTime = endTime,
                                color = selectedColor
                            )
                            navController.popBackStack()
                        },
                        enabled = title.isNotBlank()
                    ) {
                        Text(if (noteId == null) "Сохранить" else "Обновить")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title and Color Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Название заметки") },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    )
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(color, CircleShape)
                                .clickable { selectedColor = color }
                        ) {
                            if (color == selectedColor) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(6.dp)
                                        .background(Color.White, CircleShape)
                                )
                            }
                        }
                    }
                }
            }

            Divider()

            // Date and Time Section
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Start Date & Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${startDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())}, " +
                              "${startDate.format(DateTimeFormatter.ofPattern("d MMMM"))}"
                    )
                    TextButton(onClick = { showStartTimePicker = true }) {
                        Text(startTime.format(DateTimeFormatter.ofPattern("HH:mm")))
                    }
                }

                // End Date & Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${endDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())}, " +
                              "${endDate.format(DateTimeFormatter.ofPattern("d MMMM"))}"
                    )
                    TextButton(onClick = { showEndTimePicker = true }) {
                        Text(endTime.format(DateTimeFormatter.ofPattern("HH:mm")))
                    }
                }
            }
        }

        // Time Pickers
        if (showStartTimePicker) {
            TimePickerDialog(
                onDismiss = { showStartTimePicker = false },
                onConfirm = { hour, minute ->
                    startTime = LocalTime.of(hour, minute)
                    showStartTimePicker = false
                },
                initialHour = startTime.hour,
                initialMinute = startTime.minute
            )
        }

        if (showEndTimePicker) {
            TimePickerDialog(
                onDismiss = { showEndTimePicker = false },
                onConfirm = { hour, minute ->
                    endTime = LocalTime.of(hour, minute)
                    showEndTimePicker = false
                },
                initialHour = endTime.hour,
                initialMinute = endTime.minute
            )
        }
    }
}

data class Note(
    val title: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val color: Color
) 