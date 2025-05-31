package com.example.pla

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import java.time.format.TextStyle
import java.util.Locale
import androidx.lifecycle.ViewModelProvider
import com.example.pla.components.NotesDialog
import com.example.pla.components.NotesForDay
import com.example.pla.data.NoteDatabase
import com.example.pla.data.NoteEntity
import com.example.pla.data.NoteRepository
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.toArgb
import java.time.LocalTime
import androidx.compose.ui.platform.LocalFocusManager
import com.example.pla.utils.SpeechRecognizerManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import com.example.pla.utils.PermissionManager
import com.example.pla.utils.RequestRecordPermission

// --- Расширение для рисования нижней линии (бордера) ---
fun Modifier.drawBottomBorder(strokeWidth: Float, color: Color) = this.then(
    Modifier.drawBehind {
        drawLine(
            color = color,
            start = Offset(0f, size.height),
            end = Offset(size.width, size.height),
            strokeWidth = strokeWidth
        )
    }
)

// --- UI State ---
data class CalendarUiState(
    val yearMonth: YearMonth,
    val dates: List<Date>,
    val notes: Map<LocalDate, List<NoteEntity>> = emptyMap()
) {
    data class Date(
        val dayOfMonth: String,
        val isSelected: Boolean
    ) {
        companion object {
            val Empty = Date("", false)
        }
    }

    companion object {
        val Init = CalendarUiState(
            yearMonth = YearMonth.now(),
            dates = emptyList()
        )
    }
}

// --- Extension для получения дней месяца с понедельника ---
fun YearMonth.getDayOfMonthStartingFromMonday(): List<LocalDate> {
    val firstDayOfMonth = LocalDate.of(year, monthValue, 1)
    val dayOfWeek = firstDayOfMonth.dayOfWeek.value // 1 = Monday ... 7 = Sunday
    val offset = if (dayOfWeek == 1) 0 else dayOfWeek - 1
    val firstMonday = firstDayOfMonth.minusDays(offset.toLong())

    val fullList = mutableListOf<LocalDate>()
    var current = firstMonday
    repeat(42) {
        fullList.add(current)
        current = current.plusDays(1)
    }

    return fullList
}

// --- Источник данных ---
class CalendarDataSource {
    fun getDates(yearMonth: YearMonth): List<CalendarUiState.Date> {
        return yearMonth.getDayOfMonthStartingFromMonday().map { date ->
            CalendarUiState.Date(
                dayOfMonth = if (date.monthValue == yearMonth.monthValue) date.dayOfMonth.toString() else "",
                isSelected = false // выделение теперь будет из ViewModel
            )
        }
    }
}

// --- ViewModel ---
class CalendarViewModel(
    private val repository: NoteRepository
) : ViewModel() {
    private val dataSource by lazy { CalendarDataSource() }

    private val _uiState = MutableStateFlow(CalendarUiState.Init)
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private val _selectedDay = MutableStateFlow(LocalDate.now())
    val selectedDay: StateFlow<LocalDate> = _selectedDay.asStateFlow()

    private val _showNotesDialog = MutableStateFlow(false)
    val showNotesDialog: StateFlow<Boolean> = _showNotesDialog.asStateFlow()

    init {
        loadMonth(_uiState.value.yearMonth)
    }

    private fun loadMonth(yearMonth: YearMonth) {
        viewModelScope.launch {
            val dates = dataSource.getDates(yearMonth).map { date ->
                val isSelected = date.dayOfMonth.isNotEmpty() &&
                        date.dayOfMonth.toIntOrNull() == _selectedDay.value.dayOfMonth &&
                        yearMonth.monthValue == _selectedDay.value.monthValue &&
                        yearMonth.year == _selectedDay.value.year
                date.copy(isSelected = isSelected)
            }

            // Загружаем заметки для всего месяца
            val startDate = yearMonth.atDay(1)
            val endDate = yearMonth.atEndOfMonth()
            repository.getNotesForPeriod(startDate, endDate)
                .collect { notes ->
                    val notesMap = notes.groupBy { it.startDate }
                    _uiState.update {
                        it.copy(
                            yearMonth = yearMonth,
                            dates = dates,
                            notes = notesMap
                        )
                    }
                }
        }
    }

    fun toggleNotesDialog() {
        _showNotesDialog.update { !it }
    }

    fun toPreviousMonth(prevMonth: YearMonth) {
        loadMonth(prevMonth)
    }

    fun toNextMonth(nextMonth: YearMonth) {
        loadMonth(nextMonth)
    }

    fun selectDate(date: CalendarUiState.Date, yearMonth: YearMonth) {
        if (date.dayOfMonth.isEmpty()) return
        val newSelected = LocalDate.of(yearMonth.year, yearMonth.monthValue, date.dayOfMonth.toInt())
        _selectedDay.value = newSelected
        loadMonth(yearMonth)
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
}

private data class QuickNoteData(
    val title: String,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val color: Color
)

class CalendarViewModelFactory(
    private val repository: NoteRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalendarViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// --- Функция получения дней недели с понедельника ---
fun getDaysOfWeekShort(locale: Locale): List<String> {
    return DayOfWeek.values()
        .sortedBy { if (it == DayOfWeek.SUNDAY) 7 else it.value }
        .map { it.getDisplayName(TextStyle.SHORT, locale) }
}

// --- Заголовок с выбором месяца ---
@Composable
fun Header(
    yearMonth: YearMonth,
    onPreviousMonthButtonClicked: (YearMonth) -> Unit,
    onNextMonthButtonClicked: (YearMonth) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "<",
            fontSize = 24.sp,
            modifier = Modifier
                .clickable { onPreviousMonthButtonClicked(yearMonth.minusMonths(1)) }
                .padding(12.dp),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = yearMonth.month.getDisplayName(TextStyle.FULL, Locale("ru"))
                .replaceFirstChar { it.uppercase() } + " " + yearMonth.year,
            textAlign = TextAlign.Center,
            fontSize = 24.sp,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = ">",
            fontSize = 24.sp,
            modifier = Modifier
                .clickable { onNextMonthButtonClicked(yearMonth.plusMonths(1)) }
                .padding(12.dp),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

// --- Заголовок дней недели ---
@Composable
fun DaysOfWeekHeader() {
    val daysOfWeek = getDaysOfWeekShort(Locale("ru"))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBottomBorder(strokeWidth = 3f, color = Color.Gray)
    ) {
        daysOfWeek.forEachIndexed { index, day ->
            Text(
                text = day,
                color = if (index == 6) Color.Red else Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
            )
        }
    }
}

// --- Отдельный элемент даты ---
@Composable
fun ContentItem(
    date: CalendarUiState.Date,
    onClickListener: (CalendarUiState.Date) -> Unit,
    notes: List<NoteEntity> = emptyList(),
    currentDate: LocalDate,
    isSunday: Boolean = false,
    modifier: Modifier = Modifier,
    isToday: Boolean = false,
    isSelected: Boolean = false,
) {
    val borderModifier = if (isSelected) {
        Modifier.border(
            width = 1.dp,
            color = Color(0xFF1565C0),
            shape = RoundedCornerShape(8.dp)
        )
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .then(borderModifier)
            .clickable(enabled = date.dayOfMonth.isNotEmpty()) {
                onClickListener(date)
            }
    ) {
        // Число месяца
        Text(
            text = date.dayOfMonth,
            style = MaterialTheme.typography.bodyMedium,
            color = when {
                isToday -> Color.White
                isSelected -> Color(0xFF1565C0)
                isSunday -> Color.Red
                else -> Color.White
            },
            modifier = Modifier
                .padding(8.dp)
                .background(
                    color = if (isToday) Color(0xFF1565C0) else Color.Transparent,
                    shape = RoundedCornerShape(50)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )

        // Заметки
        if (date.dayOfMonth.isNotEmpty()) {
            NotesForDay(
                notes = notes,
                date = currentDate,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

// --- Контент с датами ---
@Composable
fun Content(
    dates: List<CalendarUiState.Date>,
    yearMonth: YearMonth,
    selectedDay: LocalDate,
    notes: Map<LocalDate, List<NoteEntity>>,
    onDateClickListener: (CalendarUiState.Date) -> Unit,
) {
    Column {
        var index = 0
        repeat(6) { rowIndex ->
            if (index >= dates.size) return@repeat
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (rowIndex < 5) Modifier.drawBottomBorder(1.5f, Color.Gray) else Modifier
                    )
            ) {
                repeat(7) { dayIndex ->
                    val item = if (index < dates.size) dates[index] else CalendarUiState.Date.Empty
                    val currentDate = if (item.dayOfMonth.isNotEmpty()) {
                        LocalDate.of(yearMonth.year, yearMonth.monthValue, item.dayOfMonth.toInt())
                    } else {
                        null
                    }
                    val isToday = currentDate?.equals(LocalDate.now()) ?: false
                    val isSelected = currentDate?.equals(selectedDay) ?: false

                    ContentItem(
                        date = item,
                        onClickListener = onDateClickListener,
                        notes = currentDate?.let { notes[it] } ?: emptyList(),
                        currentDate = currentDate ?: LocalDate.now(),
                        isSunday = (dayIndex == 6),
                        modifier = Modifier
                            .weight(1f)
                            .height(120.dp)
                            .padding(2.dp),
                        isToday = isToday,
                        isSelected = isSelected
                    )
                    index++
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun QuickInputSection(
    selectedDay: LocalDate,
    viewModel: CalendarViewModel,
    onAddNote: (LocalDate) -> Unit
) {
    var showQuickInput by remember { mutableStateOf(false) }
    var quickNoteText by remember { mutableStateOf("") }
    var showPermissionRequest by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Создаем и запоминаем SpeechRecognizerManager
    val speechRecognizer = remember { SpeechRecognizerManager(context) }
    val isListening by speechRecognizer.isListening.collectAsState()
    val recognizedText by speechRecognizer.recognizedText.collectAsState()
    val permissionManager = remember { PermissionManager(context) }

    // Обработка распознанного текста
    LaunchedEffect(recognizedText) {
        if (recognizedText.isNotEmpty()) {
            quickNoteText = recognizedText
        }
    }

    if (showPermissionRequest) {
        RequestRecordPermission {
            showPermissionRequest = false
            speechRecognizer.startListening()
        }
    }

    val day = selectedDay.dayOfMonth.toString()
    val monthName = selectedDay.month.getDisplayName(TextStyle.SHORT, Locale("ru"))
        .replaceFirstChar { it.uppercase() }

    // Для отслеживания двойного нажатия
    var lastTapTime by remember { mutableStateOf(0L) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showQuickInput) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = quickNoteText,
                    onValueChange = { quickNoteText = it },
                    placeholder = { Text("Например: 'Встреча в 15' или 'Обед с 13 до 14 цвет зеленый'") },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (quickNoteText.isNotBlank()) {
                                scope.launch {
                                    viewModel.saveQuickNote(quickNoteText, selectedDay)
                                    quickNoteText = ""
                                    showQuickInput = false
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                }
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedPlaceholderColor = Color.Gray,
                        unfocusedPlaceholderColor = Color.Gray
                    ),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (isListening) {
                                    speechRecognizer.stopListening()
                                } else {
                                    if (permissionManager.hasRecordAudioPermission()) {
                                        speechRecognizer.startListening()
                                    } else {
                                        showPermissionRequest = true
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                                contentDescription = if (isListening) "Остановить запись" else "Начать запись",
                                tint = if (isListening) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                    }
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .border(
                        width = 1.dp,
                        color = Color.White,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .background(Color.DarkGray.copy(alpha = 0.7f), shape = RoundedCornerShape(24.dp))
                    .combinedClickable(
                        onClick = { showQuickInput = true },
                        onDoubleClick = {
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastTapTime < 3000) { // 3 секунды для двойного нажатия
                                showQuickInput = true
                                if (permissionManager.hasRecordAudioPermission()) {
                                    speechRecognizer.startListening()
                                } else {
                                    showPermissionRequest = true
                                }
                            }
                            lastTapTime = currentTime
                        }
                    )
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "Доб. событие $day $monthName",
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        FloatingActionButton(
            onClick = { onAddNote(selectedDay) },
            containerColor = MaterialTheme.colorScheme.primary,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Add, contentDescription = "Добавить заметку")
        }
    }

    // Очистка при закрытии
    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer.destroy()
        }
    }

    // Обработка клика вне поля ввода
    BackHandler(enabled = showQuickInput) {
        showQuickInput = false
        quickNoteText = ""
        keyboardController?.hide()
        focusManager.clearFocus()
        if (isListening) {
            speechRecognizer.stopListening()
        }
    }
}

// --- Основной экран календаря с свайпом ---
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = CalendarViewModelFactory(repository = NoteRepository(NoteDatabase.getDatabase(LocalContext.current).noteDao()))
    ),
    onAddNote: (LocalDate) -> Unit,
    onEditNote: (NoteEntity) -> Unit = { }
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedDay by viewModel.selectedDay.collectAsState()
    val showNotesDialog by viewModel.showNotesDialog.collectAsState()

    var isSwiping by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = { isSwiping = false },
                        onHorizontalDrag = { change, dragAmount ->
                            if (!isSwiping && kotlin.math.abs(dragAmount) > 100f) {
                                isSwiping = true
                                if (dragAmount > 0) {
                                    viewModel.toPreviousMonth(uiState.yearMonth.minusMonths(1))
                                } else {
                                    viewModel.toNextMonth(uiState.yearMonth.plusMonths(1))
                                }
                            }
                            change.consumeAllChanges()
                        }
                    )
                }
                .background(Color(0xFF121212))
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 32.dp)
            ) {
                Header(
                    yearMonth = uiState.yearMonth,
                    onPreviousMonthButtonClicked = viewModel::toPreviousMonth,
                    onNextMonthButtonClicked = viewModel::toNextMonth
                )
                DaysOfWeekHeader()
                Content(
                    dates = uiState.dates,
                    yearMonth = uiState.yearMonth,
                    selectedDay = selectedDay,
                    notes = uiState.notes,
                    onDateClickListener = { date ->
                        viewModel.selectDate(date, uiState.yearMonth)
                        if (uiState.notes[selectedDay]?.isNotEmpty() == true) {
                            viewModel.toggleNotesDialog()
                        }
                    }
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            QuickInputSection(
                selectedDay = selectedDay,
                viewModel = viewModel,
                onAddNote = onAddNote
            )
        }

        if (showNotesDialog) {
            NotesDialog(
                notes = uiState.notes[selectedDay] ?: emptyList(),
                onDismiss = viewModel::toggleNotesDialog,
                onNoteClick = { note ->
                    viewModel.toggleNotesDialog()
                    onEditNote(note)
                }
            )
        }
    }
}
