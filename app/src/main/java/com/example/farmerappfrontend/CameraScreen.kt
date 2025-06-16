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

@Composable
fun CameraScreen(
    token: String,
    navController: NavController,
    existingAnimalIds: List<String>
) {
    var scannedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var popupMessage by remember { mutableStateOf<String?>(null) }
    var isCounting by remember { mutableStateOf(true) }
    var validatedAnimals by remember { mutableStateOf<Set<String>>(existingAnimalIds.toSet()) }
    val scope = rememberCoroutineScope()

    // Validate existing animals against backend on startup
    LaunchedEffect(Unit) {
        try {
            // Validate each existing animal ID
            existingAnimalIds.forEach { id ->
                val exists = validateAnimalIdWithBackend(token, id)
                if (!exists) {
                    // Remove from validated set if it doesn't exist in backend
                    validatedAnimals = validatedAnimals - id
                }
            }
        } catch (e: Exception) {
            popupMessage = "Error validating animals: ${e.message}"
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CameraIDReader(
            onIDDetected = { id ->
                if (id != null && !scannedIds.contains(id)) {
                    scope.launch {
                        try {
                            val exists = validateAnimalIdWithBackend(token, id)
                            validatedAnimals = if (exists) {
                                validatedAnimals + id
                            } else {
                                validatedAnimals - id
                            }
                            scannedIds = scannedIds + id
                            popupMessage = "ID: $id\nStatus: ${if (exists) "Present ✅" else "New Animal ⭐"}"
                            kotlinx.coroutines.delay(3000)
                            popupMessage = null
                        } catch (e: Exception) {
                            popupMessage = "Error validating animal: ${e.message}"
                            kotlinx.coroutines.delay(3000)
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

        // Status popup
        popupMessage?.let {
            AlertDialog(
                onDismissRequest = { popupMessage = null },
                title = { Text("Animal ID Status") },
                text = { Text(it) },
                confirmButton = {}
            )
        }

        // Done button
        Button(
            onClick = {
                val readAnimals = scannedIds.toList()
                val newAnimals = readAnimals.filterNot { validatedAnimals.contains(it) }
                navController.navigate("readAnimals/$token/${readAnimals.joinToString(",")}" +
                        "/${newAnimals.joinToString(",")}")
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
