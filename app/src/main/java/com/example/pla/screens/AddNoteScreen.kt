package com.example.pla.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

    val existingNote by viewModel.noteState.collectAsState()

    var title by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(selectedDate) }
    var endDate by remember { mutableStateOf(selectedDate) }

    // Установка начального времени
    val isToday = selectedDate == LocalDate.now()
    val initialStartTime = if (isToday) {
        val now = LocalTime.now()
        // Округляем до следующего часа через 2 часа
        LocalTime.of(now.hour + 2, 0)
    } else {
        LocalTime.of(8, 0) // 8:00 для несегодняшних дат
    }
    val initialEndTime = initialStartTime.plusHours(1) // На час позже начального времени

    var startTime by remember { mutableStateOf(initialStartTime) }
    var endTime by remember { mutableStateOf(initialEndTime) }
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

    val deleteNote: () -> Unit = {
        existingNote?.let { note ->
            viewModel.deleteNote(note)
            navController.popBackStack()
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
                .background(MaterialTheme.colorScheme.surface)
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Заголовок и цвета
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        text = if (noteId == null) "Новая заметка" else "Редактирование заметки",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("Название заметки") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = selectedColor,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge
                    )

                    ColorSelector(
                        selectedColor = selectedColor,
                        onColorSelect = { selectedColor = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        text = "Время и дата",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    DateTimeSection(
                        startDate = startDate,
                        endDate = endDate,
                        startTime = startTime,
                        endTime = endTime,
                        onStartTimeClick = { showStartTimePicker = true },
                        onEndTimeClick = { showEndTimePicker = true },
                        selectedColor = selectedColor
                    )
                }
            }
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
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Закрыть",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        actions = {
            if (noteId != null) {
                TextButton(onClick = onDelete) {
                    Text(
                        "Удалить",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            TextButton(
                onClick = onSave,
                enabled = title.isNotBlank()
            ) {
                Text(
                    if (noteId == null) "Сохранить" else "Обновить",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    )
}

@Composable
private fun ColorSelector(
    selectedColor: Color,
    onColorSelect: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Цвет заметки",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            colors.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(color, CircleShape)
                        .clickable { onColorSelect(color) }
                        .padding(2.dp)
                ) {
                    if (color == selectedColor) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                                .background(Color.White, CircleShape)
                        )
                    }
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
    onEndTimeClick: () -> Unit,
    selectedColor: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Начало",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            DateTimeRow(
                date = startDate,
                time = startTime,
                onTimeClick = onStartTimeClick,
                selectedColor = selectedColor
            )
        }
        
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Конец",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            DateTimeRow(
                date = endDate,
                time = endTime,
                onTimeClick = onEndTimeClick,
                selectedColor = selectedColor
            )
        }
    }
}

@Composable
private fun DateTimeRow(
    date: LocalDate,
    time: LocalTime,
    onTimeClick: () -> Unit,
    selectedColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru"))),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        TextButton(
            onClick = onTimeClick,
            colors = ButtonDefaults.textButtonColors(
                contentColor = selectedColor
            ),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = time.format(DateTimeFormatter.ofPattern("HH:mm")),
                style = MaterialTheme.typography.bodyLarge
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