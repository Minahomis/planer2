package com.example.pla

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
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
    val dates: List<Date>
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
class CalendarViewModel : ViewModel() {
    private val dataSource by lazy { CalendarDataSource() }

    private val _uiState = MutableStateFlow(CalendarUiState.Init)
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private val _selectedDay = MutableStateFlow(LocalDate.now())
    val selectedDay: StateFlow<LocalDate> = _selectedDay.asStateFlow()

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
            _uiState.update {
                it.copy(yearMonth = yearMonth, dates = dates)
            }
        }
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
    isSunday: Boolean = false,
    modifier: Modifier = Modifier,
    isToday: Boolean = false,
    isSelected: Boolean = false,
) {
    val borderModifier = if (isSelected) {
        Modifier.border(
            width = 2.dp,
            color = Color(0xFF1565C0),
            shape = RoundedCornerShape(8.dp)
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(borderModifier)
            .clickable(enabled = date.dayOfMonth.isNotEmpty()) {
                onClickListener(date)
            },
        contentAlignment = Alignment.TopCenter
    ) {
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
    }
}

// --- Контент с датами ---
@Composable
fun Content(
    dates: List<CalendarUiState.Date>,
    yearMonth: YearMonth,
    selectedDay: LocalDate,
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
                    val isToday = item.dayOfMonth.isNotEmpty() &&
                            yearMonth.year == LocalDate.now().year &&
                            yearMonth.monthValue == LocalDate.now().monthValue &&
                            item.dayOfMonth.toInt() == LocalDate.now().dayOfMonth
                    val isSelected = item.dayOfMonth.isNotEmpty() &&
                            yearMonth.year == selectedDay.year &&
                            yearMonth.monthValue == selectedDay.monthValue &&
                            item.dayOfMonth.toInt() == selectedDay.dayOfMonth

                    ContentItem(
                        date = item,
                        onClickListener = onDateClickListener,
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

// --- Основной экран календаря с свайпом ---
@Composable
fun CalendarScreen(viewModel: CalendarViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedDay by viewModel.selectedDay.collectAsState()

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
                .padding(bottom = 48.dp)
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
                    onDateClickListener = { date ->
                        viewModel.selectDate(date, uiState.yearMonth)
                    }
                )
            }
        }

        // Нижняя панель с текстом и круглой кнопкой
        val day = selectedDay.dayOfMonth.toString()
        val monthName = selectedDay.month.getDisplayName(TextStyle.SHORT, Locale("ru"))
            .replaceFirstChar { it.uppercase() }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "Доб. событие $day $monthName",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF1565C0), shape = CircleShape)
                    .clickable {
                        // TODO: обработка нажатия на кнопку добавления события
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Добавить событие",
                    tint = Color.White
                )
            }
        }
    }
}
