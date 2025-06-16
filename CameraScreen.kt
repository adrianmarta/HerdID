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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.navigation.NavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

@Composable
fun CameraScreen(
    token: String,
    navController: NavController
) {
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var detectedAnimalId by remember { mutableStateOf<String?>(null) }
    var animalDetails by remember { mutableStateOf<Animal?>(null) }
    var isAnimalValid by remember { mutableStateOf<Boolean?>(null) }
    var cooldownActive by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        CameraIDReader(
            onIDDetected = { id ->
                if (!cooldownActive && id != null) {
                    cooldownActive = true
                    detectedAnimalId = id
                    // Fetch animal details and validate
                    scope.launch {
                        try {
                            val isValid = validateAnimalIdWithBackend(token, id)
                            isAnimalValid = isValid
                            if (isValid) {
                                // Fetch additional animal details
                                animalDetails = fetchAnimalDetails(token, id)
                            }
                        } catch (e: Exception) {
                            errorMessage = e.message
                        } finally {
                            // Cooldown period of 3 seconds
                            kotlinx.coroutines.delay(3000)
                            cooldownActive = false
                        }
                    }
                }
            },
            onError = { error ->
                if (!cooldownActive) {
                    errorMessage = error
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Show popup for results
        if (detectedAnimalId != null && isAnimalValid != null) {
            AlertDialog(
                onDismissRequest = {
                    detectedAnimalId = null
                    isAnimalValid = null
                    animalDetails = null
                },
                title = { Text("Animal ID Validation") },
                text = {
                    Text(
                        text = if (isAnimalValid == true) {
                            "Animal ID: $detectedAnimalId\nStatus: Valid ✅\n" +
                                    "Gender: ${animalDetails?.gender ?: "Unknown"}\n" +
                                    "Birth Date: ${animalDetails?.birthDate ?: "Unknown"}"
                        } else {
                            "Animal ID: $detectedAnimalId\nStatus: Invalid ❌"
                        }
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        detectedAnimalId = null
                        isAnimalValid = null
                        animalDetails = null
                    }) {
                        Text("OK")
                    }
                }
            )
        }

        // Show error messages
        errorMessage?.let {
            AlertDialog(
                onDismissRequest = { errorMessage = null },
                title = { Text("Error") },
                text = { Text(it) },
                confirmButton = {
                    Button(onClick = { errorMessage = null }) {
                        Text("OK")
                    }
                }
            )
        }

        // Add a back button
        IconButton(
            onClick = { navController.navigateUp() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface
            )
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
        val response = RetrofitClient.apiService.getAnimalDetails(animalId, "Bearer $token")
        if (response.isSuccessful) {
            response.body()
        } else {
            throw Exception("Failed to fetch animal details: ${response.message()}")
        }
    } catch (e: Exception) {
        throw Exception("Error fetching animal details: ${e.message}")
    }
}
