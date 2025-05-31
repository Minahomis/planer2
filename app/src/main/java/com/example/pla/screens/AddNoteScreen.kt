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
import kotlinx.coroutines.launch

private val colors = listOf(
    Color(0xFF1A73E8), // Blue
    Color(0xFFDB4437), // Red
    Color(0xFF0F9D58), // Green
    Color(0xFFF4B400), // Yellow
    Color(0xFF7B1FA2)  // Purple
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNoteScreen(
    navController: NavController,
    selectedDate: LocalDate = LocalDate.now(),
    noteId: Long? = null
) {
    val context = LocalContext.current
    val database = remember { NoteDatabase.getDatabase(context) }
    val repository = remember { NoteRepository(database.noteDao()) }
    val viewModel: AddNoteViewModel = viewModel(
        factory = AddNoteViewModelFactory(repository, noteId)
    )
    val scope = rememberCoroutineScope()

    val existingNote by viewModel.noteState.collectAsState()

    var title by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(selectedDate) }
    var endDate by remember { mutableStateOf(selectedDate) }
    var startTime by remember { mutableStateOf(LocalTime.now()) }
    var endTime by remember { mutableStateOf(LocalTime.now().plusHours(1)) }
    var selectedColor by remember { mutableStateOf(colors[0]) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

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

    val saveNote: () -> Unit = {
        scope.launch {
            viewModel.saveNote(
                title = title,
                startDate = startDate,
                endDate = endDate,
                startTime = startTime,
                endTime = endTime,
                color = selectedColor
            )
            navController.popBackStack()
        }
    }

    val deleteNote: () -> Unit = {
        scope.launch {
            existingNote?.let { note ->
                viewModel.deleteNote(note)
                navController.popBackStack()
            }
        }
    }

    Scaffold(
        topBar = {
            AddNoteTopBar(
                noteId = noteId,
                title = title,
                onClose = { navController.popBackStack() },
                onSave = saveNote,
                onDelete = deleteNote
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
            NoteInputSection(
                title = title,
                onTitleChange = { title = it },
                selectedColor = selectedColor,
                onColorSelect = { selectedColor = it }
            )

            Divider()

            DateTimeSection(
                startDate = startDate,
                endDate = endDate,
                startTime = startTime,
                endTime = endTime,
                onStartTimeClick = { showStartTimePicker = true },
                onEndTimeClick = { showEndTimePicker = true }
            )
        }

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddNoteTopBar(
    noteId: Long?,
    title: String,
    onClose: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    TopAppBar(
        title = { },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Закрыть")
            }
        },
        actions = {
            if (noteId != null) {
                TextButton(onClick = onDelete) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            }
            TextButton(
                onClick = onSave,
                enabled = title.isNotBlank()
            ) {
                Text(if (noteId == null) "Сохранить" else "Обновить")
            }
        }
    )
}

@Composable
private fun NoteInputSection(
    title: String,
    onTitleChange: (String) -> Unit,
    selectedColor: Color,
    onColorSelect: (Color) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            placeholder = { Text("Название заметки") },
            modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent
            )
        )
        
        ColorSelector(
            selectedColor = selectedColor,
            onColorSelect = onColorSelect
        )
    }
}

@Composable
private fun ColorSelector(
    selectedColor: Color,
    onColorSelect: (Color) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(start = 8.dp)
    ) {
        colors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(color, CircleShape)
                    .clickable { onColorSelect(color) }
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

@Composable
private fun DateTimeSection(
    startDate: LocalDate,
    endDate: LocalDate,
    startTime: LocalTime,
    endTime: LocalTime,
    onStartTimeClick: () -> Unit,
    onEndTimeClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DateTimeRow(
            date = startDate,
            time = startTime,
            onTimeClick = onStartTimeClick
        )
        
        DateTimeRow(
            date = endDate,
            time = endTime,
            onTimeClick = onEndTimeClick
        )
    }
}

@Composable
private fun DateTimeRow(
    date: LocalDate,
    time: LocalTime,
    onTimeClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())}, " +
                  "${date.format(DateTimeFormatter.ofPattern("d MMMM"))}"
        )
        TextButton(onClick = onTimeClick) {
            Text(time.format(DateTimeFormatter.ofPattern("HH:mm")))
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