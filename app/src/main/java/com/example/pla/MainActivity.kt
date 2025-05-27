package com.example.pla

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.pla.ui.theme.PlaTheme

class MainActivity : ComponentActivity() {
    private val viewModel: CalendarViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PlaTheme {
                CalendarScreen(viewModel = viewModel)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CalendarScreenPreview() {
    PlaTheme {
        CalendarScreen(viewModel = CalendarViewModel())
    }
}