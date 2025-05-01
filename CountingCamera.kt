package com.example.farmerappfrontend

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue

import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch


import androidx.compose.foundation.layout.*

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier


@Composable
fun CountingCamera(
    token: String,
    animalIds: List<String>,
    onComplete: (List<String>) -> Unit // Callback for not-read IDs
) {
    var scannedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var popupMessage by remember { mutableStateOf<String?>(null) }
    var isCounting by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        if (isCounting) {
            CameraIDReader(
                onIDDetected = { id ->
                    if (id != null && !scannedIds.contains(id)) {
                        scannedIds = scannedIds + id
                        val isPresent = animalIds.contains(id)
                        popupMessage = "ID: $id\nStatus: ${if (isPresent) "Present ✅" else "Not Present ❌"}"
                        scope.launch {
                            kotlinx.coroutines.delay(5000) // Show popup for 5 seconds
                            popupMessage = null
                        }
                    }
                },
                onError = { error ->
                    popupMessage = "Error: $error"
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        popupMessage?.let {
            AlertDialog(
                onDismissRequest = { popupMessage = null },
                title = { Text("Animal ID Status") },
                text = { Text(it) },
                confirmButton = {}
            )
        }

        Button(
            onClick = {
                isCounting = false
                val notReadIds = animalIds.filterNot { scannedIds.contains(it) }
                onComplete(notReadIds) // Send not-read IDs back
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Text("Done")
        }
    }
}
