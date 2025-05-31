package com.example.pla.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pla.data.NoteEntity
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesDialog(
    notes: List<NoteEntity>,
    onDismiss: () -> Unit,
    onNoteClick: (NoteEntity) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Заметки",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notes.sortedBy { it.startTime }) { note ->
                        NoteListItem(
                            note = note,
                            onClick = { onNoteClick(note) }
                        )
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 8.dp)
                ) {
                    Text("Закрыть")
                }
            }
        }
    }
}

@Composable
private fun NoteListItem(
    note: NoteEntity,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Время начала
        Text(
            text = note.startTime.format(DateTimeFormatter.ofPattern("HH:mm")),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        // Цветная вертикальная линия
        Box(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .width(4.dp)
                .height(24.dp)
                .background(Color(note.color), RoundedCornerShape(2.dp))
        )

        Column(modifier = Modifier.weight(1f)) {
            // Название заметки
            Text(
                text = note.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            // Время
            Text(
                text = "${note.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} - " +
                        note.endTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
} 