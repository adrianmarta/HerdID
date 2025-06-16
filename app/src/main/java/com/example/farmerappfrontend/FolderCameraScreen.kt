package com.example.farmerappfrontend

import androidx.compose.foundation.layout.Box
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderCameraScreen(
    token: String,
    folderId: String,
    navController: NavController,
    existingAnimalIds: List<String>
) {
    var scannedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var popupMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        CameraIDReader(
            onIDDetected = { id ->
                if (id != null && !scannedIds.contains(id)) {
                    scannedIds = scannedIds + id
                    scope.launch {
                        val status = if (existingAnimalIds.contains(id)) {
                            "In Folder ✅"
                        } else {
                            try {
                                val existsGloballyResponse = RetrofitClient.apiService.checkAnimalExists(id, "Bearer $token")
                                if (existsGloballyResponse.isSuccessful && existsGloballyResponse.body() == true) {
                                    "Not in Folder (Exists Globally) ⭐"
                                } else {
                                    "New Animal ✨"
                                }
                            } catch (e: Exception) {
                                Log.e("FolderCameraScreen", "Error checking global existence for $id: ${e.message}")
                                "Error checking status ⚠️"
                            }
                        }
                        popupMessage = "ID: $id\nStatus: $status"
                        delay(3000) // Show popup for 3 seconds
                        popupMessage = null
                    }
                }
            },
            onError = { error ->
                popupMessage = "Camera Error: $error"
            },
            modifier = Modifier.fillMaxSize()
        )

        // Status popup
        popupMessage?.let { message ->
            AlertDialog(
                onDismissRequest = { popupMessage = null },
                title = { Text("Animal ID Status") },
                text = { Text(message) },
                confirmButton = {}
            )
        }

        // Done button
        Button(
            onClick = {
                // Collect scanned IDs and their determined status (will refine this for FolderReadAnimalsScreen)
                val scannedAnimalsWithStatus = scannedIds.map { id ->
                    val status = when {
                        existingAnimalIds.contains(id) -> "In Folder"
                        // Note: Global existence check needs to be done again or passed from detection
                        // For now, we'll pass the IDs and determine detailed status in the next screen
                        else -> "Needs Global Check"
                    }
                    id // Just passing IDs for now
                }.toList()

                // TODO: Implement navigation to FolderReadAnimalsScreen
                // Example navigation (route needs to be defined in MainActivity.kt):
                // val scannedIdsJson = Gson().toJson(scannedAnimalsWithStatus)
                // navController.navigate("folderReadAnimals/$token/$folderId?scannedIds=${scannedIdsJson}")

                val scannedIdsString = scannedIds.joinToString(",")
                navController.navigate("folderReadAnimals/$token/$folderId?scannedAnimalIds=${scannedIdsString}")
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Text("Done (${scannedIds.size} animals read)")
        }
    }
} 