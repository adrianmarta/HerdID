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

@Composable
fun CameraScreen(
    token: String,
    onAnimalDetected: ((String?, Boolean) -> Unit)? = null
) {
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var detectedAnimalId by remember { mutableStateOf<String?>(null) }
    var isAnimalValid by remember { mutableStateOf<Boolean?>(null) }
    var noIdFoundMessageShown by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        CameraIDReader(
            onIDDetected = { id ->
                detectedAnimalId = id
                if (id != null) {
                    noIdFoundMessageShown = false // Reset "No ID found" message
                    // Validate the detected ID with the backend
                    CoroutineScope(Dispatchers.IO).launch {
                        validateAnimalIdWithBackend(token, id) { isValid, error ->
                            isAnimalValid = isValid
                            errorMessage = error
                        }
                    }
                } else if (!noIdFoundMessageShown) {
                    errorMessage = "No ID detected"
                    noIdFoundMessageShown = true // Show "No ID found" only once
                }
            },
            onError = { error ->
                errorMessage = error
            },
            modifier = Modifier.fillMaxSize()
        )

        // Show popup for results
        if (detectedAnimalId != null && isAnimalValid != null) {
            AlertDialog(
                onDismissRequest = {
                    detectedAnimalId = null
                    isAnimalValid = null
                },
                title = { Text("Animal ID Validation") },
                text = {
                    Text(
                        text = if (isAnimalValid == true) {
                            "Animal ID: $detectedAnimalId\nStatus: Valid ✅"
                        } else {
                            "Animal ID: $detectedAnimalId\nStatus: Invalid ❌"
                        }
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        detectedAnimalId = null
                        isAnimalValid = null
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
    }
}

suspend fun validateAnimalIdWithBackend(
    token: String,
    animalId: String,
    onResult: (Boolean, String?) -> Unit
) {
    try {
        val response = RetrofitClient.apiService.checkAnimalExists(animalId, "Bearer $token")
        if (response.isSuccessful) {
            val isValid = response.body() ?: false
            onResult(isValid, null)
        } else {
            onResult(false, "Failed to validate animal ID: ${response.message()}")
        }
    } catch (e: Exception) {
        onResult(false, "Error: ${e.message}")
    }
}