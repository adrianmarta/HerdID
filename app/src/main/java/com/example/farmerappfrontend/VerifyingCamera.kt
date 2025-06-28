package com.example.farmerappfrontend

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch


import androidx.compose.foundation.layout.*

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.text.font.FontWeight

@Composable
fun CountingCamera(
    token: String,
    animalIds: List<String>,
    onComplete: (List<String>) -> Unit
) {
    var scannedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var popupMessage by remember { mutableStateOf<String?>(null) }
    var isCounting by remember { mutableStateOf(true) }
    var animalDetails by remember { mutableStateOf<AnimalDetails?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun vibrate(duration: Long) {
        try {
            val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(duration)
            }
        } catch (_: Exception) {}
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isCounting) {
            CameraIDReader(
                onIDDetected = { id ->
                    if (id != null) {

                        val isPresent = animalIds.any { it.trim() == id.trim() }
                        if (isPresent) {
                            vibrate(100)
                            scope.launch {
                                try {
                                    val response = RetrofitClient.apiService.getAnimalDetails(id.trim(), token)
                                    if (response.isSuccessful) {
                                        animalDetails = response.body()
                                        popupMessage = null
                                    } else {
                                        animalDetails = null
                                        popupMessage = "ID: $id\nStatus: Present ✅\n(Details unavailable)"
                                    }
                                } catch (e: Exception) {
                                    animalDetails = null
                                    popupMessage = "ID: $id\nStatus: Present ✅\n(Details unavailable)"
                                }
                                kotlinx.coroutines.delay(2000)
                                animalDetails = null
                                popupMessage = null
                            }
                        } else {
                            vibrate(400)
                            animalDetails = null
                            popupMessage = "ID: $id\nStatus: Not Present ❌"
                            scope.launch {
                                kotlinx.coroutines.delay(2000)
                                popupMessage = null
                            }
                        }
                    }
                },
                onError = { error ->
                    popupMessage = "Error: $error"
                },
                onPartialIdDetected = {  },
                modifier = Modifier.fillMaxSize()

            )
        }

        // Show animal details
        animalDetails?.let { details ->
            AlertDialog(
                onDismissRequest = { animalDetails = null },
                title = { 
                    Text(
                        "Animal Details",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DetailRow("ID", details.id)
                        DetailRow("Gender", details.gender)
                        DetailRow("Birth Date", details.birthDate)
                        DetailRow("Species", details.species)
                        DetailRow("Produces Milk", if (details.producesMilk) "Yes" else "No")
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { animalDetails = null },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Close")
                    }
                }
            )
        }

        popupMessage?.let {
            AlertDialog(
                onDismissRequest = { popupMessage = null },
                title = { 
                    Text(
                        "Animal ID Status",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                text = { 
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = { popupMessage = null },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Close")
                    }
                }
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

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
