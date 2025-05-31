package com.example.pla.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    initialHour: Int = 9,
    initialMinute: Int = 0
) {
    var selectedHour by remember { mutableStateOf(initialHour) }
    var selectedMinute by remember { mutableStateOf(initialMinute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Выберите время",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        },
        text = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hours
                NumberPicker(
                    value = selectedHour,
                    onValueChange = { selectedHour = it },
                    range = 0..23,
                    modifier = Modifier.weight(1f)
                )
                
                Text(
                    ":",
                    modifier = Modifier.padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.headlineMedium
                )
                
                // Minutes
                NumberPicker(
                    value = selectedMinute,
                    onValueChange = { selectedMinute = it },
                    range = 0..59,
                    modifier = Modifier.weight(1f)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedHour, selectedMinute) }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NumberPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = value - range.first + 1 + range.count() * 100
    )
    val coroutineScope = rememberCoroutineScope()
    val itemHeight = 40.dp
    val visibleItems = 3

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Center indicator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .height(itemHeight * visibleItems)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            userScrollEnabled = true,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
        ) {
            // Create "infinite" list by repeating values
            items(range.count() * 200 + (visibleItems - 1) * 2) { index ->
                val actualIndex = index - (visibleItems - 1)
                val rangeSize = range.count()
                val number = when {
                    actualIndex < 0 -> range.last - (-actualIndex - 1) % rangeSize
                    else -> range.first + (actualIndex % rangeSize)
                }
                
                if (number in range) {
                    val formattedNumber = String.format("%02d", number)
                    Box(
                        modifier = Modifier
                            .height(itemHeight)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        val isSelected = index == listState.firstVisibleItemIndex + 1
                        Text(
                            text = formattedNumber,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.alpha(if (isSelected) 1f else 0.3f)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(itemHeight))
                }
            }
        }
    }

    LaunchedEffect(listState.firstVisibleItemIndex) {
        val actualIndex = (listState.firstVisibleItemIndex - 1)
        val rangeSize = range.count()
        val selectedValue = when {
            actualIndex < 0 -> range.last - (-actualIndex - 1) % rangeSize
            else -> range.first + (actualIndex % rangeSize)
        }
        if (selectedValue in range) {
            onValueChange(selectedValue)
        }
    }

    DisposableEffect(listState) {
        onDispose {
            coroutineScope.launch {
                listState.animateScrollToItem(listState.firstVisibleItemIndex)
            }
        }
    }
} 