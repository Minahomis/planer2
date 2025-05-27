package com.example.pla

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
                isSelected = date.isEqual(LocalDate.now()) && date.monthValue == yearMonth.monthValue
            )
        }
    }
}

// --- ViewModel ---
class CalendarViewModel : ViewModel() {
    private val dataSource by lazy { CalendarDataSource() }

    private val _uiState = MutableStateFlow(CalendarUiState.Init)
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        loadMonth(_uiState.value.yearMonth)
    }

    private fun loadMonth(yearMonth: YearMonth) {
        viewModelScope.launch {
            val dates = dataSource.getDates(yearMonth)
            _uiState.update {
                it.copy(yearMonth = yearMonth, dates = dates)
            }
        }
    }

    fun toPreviousMonth(prevMonth: YearMonth) = loadMonth(prevMonth)
    fun toNextMonth(nextMonth: YearMonth) = loadMonth(nextMonth)
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

// --- Контент с датами ---
@Composable
fun Content(
    dates: List<CalendarUiState.Date>,
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
                    ContentItem(
                        date = item,
                        onClickListener = onDateClickListener,
                        isSunday = (dayIndex == 6),
                        modifier = Modifier
                            .weight(1f)
                            .height(120.dp)
                            .padding(2.dp)
                    )
                    index++
                }
            }
        }
    }
}

// --- Отдельный элемент даты ---
@Composable
fun ContentItem(
    date: CalendarUiState.Date,
    onClickListener: (CalendarUiState.Date) -> Unit,
    isSunday: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = if (date.isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
            )
            .clickable(enabled = date.dayOfMonth.isNotEmpty()) {
                onClickListener(date)
            },
        contentAlignment = Alignment.TopCenter
    ) {
        Text(
            text = date.dayOfMonth,
            style = MaterialTheme.typography.bodyMedium,
            color = when {
                date.isSelected -> MaterialTheme.colorScheme.onSecondaryContainer
                isSunday -> Color.Red
                else -> Color.White
            },
            modifier = Modifier.padding(10.dp)
        )
    }
}

// --- Основной экран календаря с свайпом ---
@Composable
fun CalendarScreen(viewModel: CalendarViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var isSwiping by remember { mutableStateOf(false) } // Флаг для предотвращения множественных свайпов

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = { isSwiping = false }, // Сбрасываем флаг после завершения свайпа
                    onHorizontalDrag = { change, dragAmount ->
                        if (!isSwiping && kotlin.math.abs(dragAmount) > 100f) { // Увеличенный порог свайпа
                            isSwiping = true // Устанавливаем флаг, чтобы предотвратить повторные свайпы
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
                .padding(top = 32.dp) // Увеличенный отступ сверху (16.dp + 16.dp)
        ) {
            Header(
                yearMonth = uiState.yearMonth,
                onPreviousMonthButtonClicked = viewModel::toPreviousMonth,
                onNextMonthButtonClicked = viewModel::toNextMonth
            )
            DaysOfWeekHeader()
            Content(
                dates = uiState.dates,
                onDateClickListener = {
                    // Обработка клика по дате
                }
            )
        }
    }
}