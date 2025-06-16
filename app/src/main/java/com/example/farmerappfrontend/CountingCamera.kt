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
    var animalDetails by remember { mutableStateOf<AnimalDetails?>(null) }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        if (isCounting) {
            CameraIDReader(
                onIDDetected = { id ->
                    if (id != null && !scannedIds.contains(id)) {
                        scannedIds = scannedIds + id
                        val isPresent = animalIds.any { it.trim() == id.trim() }
                        if (isPresent) {
                            // Fetch animal details
                            scope.launch {
                                try {
                                    val response = RetrofitClient.apiService.getAnimalDetails(id.trim(), token)
                                    if (response.isSuccessful) {
                                        animalDetails = response.body()
                                        popupMessage = null // Hide the old popup
                                    } else {
                                        animalDetails = null
                                        popupMessage = "ID: $id\nStatus: Present ✅\n(Details unavailable)"
                                    }
                                } catch (e: Exception) {
                                    animalDetails = null
                                    popupMessage = "ID: $id\nStatus: Present ✅\n(Details unavailable)"
                                }
                                // Hide after 5 seconds
                                kotlinx.coroutines.delay(5000)
                                animalDetails = null
                                popupMessage = null
                            }
                        } else {
                            animalDetails = null
                            popupMessage = "ID: $id\nStatus: Not Present ❌"
                            scope.launch {
                                kotlinx.coroutines.delay(5000)
                                popupMessage = null
                            }
                        }
                    }
                },
                onError = { error ->
                    popupMessage = "Error: $error"
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Show animal details dialog if present
        animalDetails?.let { details ->
            AlertDialog(
                onDismissRequest = { animalDetails = null },
                title = { Text("Animal Details") },
                text = {
                    Column {
                        Text("ID: ${details.id}")
                        Text("Gender: ${details.gender}")
                        Text("Birth Date: ${details.birthDate}")
                        Text("Species: ${details.species}")
                        Text("Produces Milk: ${if (details.producesMilk) "Yes" else "No"}")
                    }
                },
                confirmButton = {}
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
