package com.example.pla.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat

class PermissionManager(private val context: Context) {
    fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
}

@Composable
fun RequestRecordPermission(
    onPermissionGranted: () -> Unit,
    onDismiss: () -> Unit
) {
    var hasPermission by remember { mutableStateOf(false) }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasPermission = isGranted
            if (isGranted) {
                onPermissionGranted()
            } else {
                onDismiss()
            }
        }
    )

    LaunchedEffect(Unit) {
        launcher.launch(Manifest.permission.RECORD_AUDIO)
    }
} 