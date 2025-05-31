package com.example.pla.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pla.data.NoteEntity
import java.time.LocalDate

@Composable
fun SingleDayNoteItem(
    note: NoteEntity,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Цветная вертикальная линия
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(Color(note.color))
        )
        
        // Текст заметки
        Text(
            text = note.title,
            color = Color.White,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(start = 4.dp)
                .weight(1f)
        )
    }
}

@Composable
fun MultiDayNoteItem(
    note: NoteEntity,
    isStart: Boolean,
    isEnd: Boolean,
    modifier: Modifier = Modifier
) {
    val shape = when {
        isStart && isEnd -> RoundedCornerShape(4.dp)
        isStart -> RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp)
        isEnd -> RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)
        else -> RoundedCornerShape(0.dp)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(18.dp)
            .clip(shape)
            .background(Color(note.color).copy(alpha = 0.3f))
    ) {
        if (isStart) {
            Text(
                text = note.title,
                color = Color.White,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .align(Alignment.CenterStart)
            )
        }
    }
}

@Composable
fun NotesForDay(
    notes: List<NoteEntity>,
    date: LocalDate,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Сортируем заметки по времени начала
        val sortedNotes = notes.sortedBy { it.startTime }
        
        sortedNotes.forEach { note ->
            if (note.startDate == note.endDate) {
                // Однодневная заметка
                SingleDayNoteItem(note = note)
            } else {
                // Многодневная заметка
                MultiDayNoteItem(
                    note = note,
                    isStart = date == note.startDate,
                    isEnd = date == note.endDate
                )
            }
        }
    }
} 