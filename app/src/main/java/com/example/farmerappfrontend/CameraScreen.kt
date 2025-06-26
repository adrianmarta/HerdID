package com.example.farmerappfrontend

import android.Manifest
import android.content.pm.PackageManager
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.navigation.NavController
import com.example.farmerappfrontend.levenshtein
import com.example.farmerappfrontend.areOcrSimilar
import androidx.lifecycle.viewmodel.compose.viewModel
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.content.Context

@Composable
fun CameraScreen(
    token: String,
    navController: NavController,
    existingAnimalIds: List<String>,
    cameraViewModel: CameraViewModel
) {
    val scannedIds by cameraViewModel.scannedIds.collectAsState()
    var popupMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var partialId by remember { mutableStateOf<String?>(null) }
    var manualIdInput by remember { mutableStateOf("") }
    var showManualDialog by remember { mutableStateOf(false) }
    var closestMatch by remember { mutableStateOf<String?>(null) }
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

    fun findClosestMatch(id: String): String? {
        return existingAnimalIds.minByOrNull { levenshtein(it, id) }
    }

    fun processScannedId(id: String) {
        if (id.isBlank()) return
        val trimmedId = id.trim()
        if (scannedIds.contains(trimmedId)) {
            popupMessage = "ID: $trimmedId\nStatus: Already Scanned"
            return
        }
        cameraViewModel.addScannedId(trimmedId)
        scope.launch {
            try {
                val exists = validateAnimalIdWithBackend(token, trimmedId)
                popupMessage = "ID: $trimmedId\nStatus: ${if (exists) "Present ✅" else "New Animal ⭐"}"
                kotlinx.coroutines.delay(3000)
                popupMessage = null
            } catch (e: Exception) {
                popupMessage = "Error validating animal: ${e.message}"
                kotlinx.coroutines.delay(3000)
                popupMessage = null
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CameraIDReader(
            onIDDetected = { id ->
                if (id != null) {
                    vibrate(100)
                    processScannedId(id)
                }
            },
            onPartialIdDetected = { partial ->
                vibrate(400)
                partialId = partial
                manualIdInput = partial
                closestMatch = findClosestMatch(partial)
                showManualDialog = true
            },
            onError = { error ->
                popupMessage = "Error: $error"
            },
            modifier = Modifier.fillMaxSize()
        )
        popupMessage?.let {
            AlertDialog(
                onDismissRequest = { popupMessage = null },
                title = { Text("Animal ID Status") },
                text = { Text(it) },
                confirmButton = {}
            )
        }

        if (showManualDialog && partialId != null) {
            AlertDialog(
                onDismissRequest = { showManualDialog = false },
                title = { Text("ID Not Found / Partial ID Detected") },
                text = {
                    Column {
                        Text("Partial/Scanned ID: $partialId")
                        closestMatch?.let {
                            Text("Closest match: $it")
                            Button(onClick = {
                                processScannedId(it) // Use suggested ID
                                showManualDialog = false
                            }) { Text("Use $it") }
                        }
                        OutlinedTextField(
                            value = manualIdInput,
                            onValueChange = { manualIdInput = it },
                            label = { Text("Enter/Correct ID") }
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        processScannedId(manualIdInput) // Use manual input
                        showManualDialog = false
                    }) { Text("Confirm") }
                },
                dismissButton = {
                    Button(onClick = { showManualDialog = false }) { Text("Cancel") }
                }
            )
        }

        // Done button
        Button(
            onClick = {
                navController.navigate("readAnimals/$token")
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Text("Done (${scannedIds.size} animals read)")
        }
    }
}

suspend fun validateAnimalIdWithBackend(
    token: String,
    animalId: String
): Boolean {
    return try {
        val response = RetrofitClient.apiService.checkAnimalExists(animalId, "Bearer $token")
        if (response.isSuccessful) {
            response.body() ?: false
        } else {
            throw Exception("Failed to validate animal ID: ${response.message()}")
        }
    } catch (e: Exception) {
        throw Exception("Error: ${e.message}")
    }
}

suspend fun fetchAnimalDetails(
    token: String,
    animalId: String
): Animal? {
    return try {
        val response = RetrofitClient.apiService.getAnimal(animalId, "Bearer $token")
        if (response.isSuccessful) {
            response.body()
        } else {
            throw Exception("Failed to fetch animal details: ${response.message()}")
        }
    } catch (e: Exception) {
        throw Exception("Error fetching animal details: ${e.message}")
    }
}
