package com.example.pla.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.pla.screens.AddNoteScreen
import com.example.pla.CalendarScreen
import java.time.LocalDate

@Composable
fun Navigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.CALENDAR
    ) {
        composable(NavRoutes.CALENDAR) {
            CalendarScreen(
                onAddNote = { date ->
                    navController.navigate(NavRoutes.addNote(date.toString()))
                },
                onEditNote = { note ->
                    navController.navigate(NavRoutes.editNote(note.id))
                }
            )
        }

        composable(
            route = NavRoutes.ADD_NOTE,
            arguments = listOf(
                navArgument("date") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val dateStr = backStackEntry.arguments?.getString("date") ?: LocalDate.now().toString()
            AddNoteScreen(
                navController = navController,
                selectedDate = LocalDate.parse(dateStr)
            )
        }

        composable(
            route = NavRoutes.EDIT_NOTE,
            arguments = listOf(
                navArgument("noteId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: return@composable
            AddNoteScreen(
                navController = navController,
                noteId = noteId
            )
        }
    }
} 