package com.example.farmerappfrontend

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.util.Log
import com.google.gson.Gson
import androidx.compose.material3.OutlinedTextField
import com.example.farmerappfrontend.levenshtein
import com.example.farmerappfrontend.areOcrSimilar
import androidx.compose.runtime.collectAsState
import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderCameraScreen(
    token: String,
    folderId: String,
    navController: NavController,
    existingAnimalIds: List<String>,
    folderCameraViewModel: FolderCameraViewModel
) {
    val scannedIds by folderCameraViewModel.scannedIds.collectAsState()
    var popupMessage by remember { mutableStateOf<String?>(null) }
    var animalDetails by remember { mutableStateOf<AnimalDetails?>(null) }
    var partialId by remember { mutableStateOf<String?>(null) }
    var manualIdInput by remember { mutableStateOf("") }
    var showManualDialog by remember { mutableStateOf(false) }
    var closestMatches by remember { mutableStateOf<List<String>>(emptyList()) }
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

    fun findClosestMatch(id: String): String? {
        return existingAnimalIds.minByOrNull { levenshtein(it, id) }
    }

    fun processScannedId(id: String) {
        if (id.isBlank()) return

        val trimmedId = id.trim()

        if (scannedIds.contains(trimmedId)) {
            return
        }

        folderCameraViewModel.addScannedId(trimmedId)

        scope.launch {
            val isInFolder = existingAnimalIds.contains(trimmedId)
            if (isInFolder) {
                vibrate(100)
                popupMessage = "ID: $trimmedId\nStatus: Present ⭐"
                delay(5000)
                animalDetails = null
                popupMessage = null
            } else {
                vibrate(400)
                animalDetails = null
                try {
                    val existsGloballyResponse = RetrofitClient.apiService.checkAnimalExists(trimmedId, "Bearer $token")
                    if (existsGloballyResponse.isSuccessful && existsGloballyResponse.body() == true) {
                        popupMessage = "ID: $trimmedId\nStatus: Not in Folder (Exists Globally) ⭐"
                    } else {
                        popupMessage = "ID: $trimmedId\nStatus: New Animal ✨"
                    }
                } catch (e: Exception) {
                    Log.e("FolderCameraScreen", "Error checking global existence for $trimmedId: ${e.message}")
                    popupMessage = "ID: $trimmedId\nStatus: Error checking status ⚠️"
                }
                delay(3000)
                popupMessage = null
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CameraIDReader(
            onIDDetected = { id ->
                if (id != null) processScannedId(id)
            },
            onError = { error ->
                popupMessage = "Camera Error: $error"
            },
            onPartialIdDetected = { partial ->
                vibrate(100)
                vibrate(100)
                partialId = partial
                manualIdInput = partial
                closestMatches = closestMatches(partial, existingAnimalIds, maxSuggestions = 3)
                showManualDialog = true
            },
            modifier = Modifier.fillMaxSize()
        )

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

        if (showManualDialog && partialId != null) {
            AlertDialog(
                onDismissRequest = { showManualDialog = false },
                title = { Text("ID Not Found / Partial ID Detected") },
                text = {
                    Column {
                        Text("Partial/Scanned ID: $partialId")
                        if (closestMatches.isNotEmpty()) {
                            Text("Closest matches:")
                            closestMatches.forEach { match ->
                                Button(
                                    onClick = {
                                        processScannedId(match)
                                        showManualDialog = false
                                    },
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) { Text(match) }
                            }
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
                        processScannedId(manualIdInput)
                        showManualDialog = false
                    }) { Text("Confirm") }
                },
                dismissButton = {
                    Button(onClick = { showManualDialog = false }) { Text("Cancel") }
                }
            )
        }

        popupMessage?.let { message ->
            AlertDialog(
                onDismissRequest = { popupMessage = null },
                title = { Text("Animal ID Status") },
                text = { Text(message) },
                confirmButton = {}
            )
        }

        Button(
            onClick = {
                navController.navigate("folderReadAnimals/$token/$folderId")
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Text("Done (${scannedIds.size} animals read)")
        }
    }
} 