package com.example.farmerappfrontend

import android.Manifest
import android.content.pm.PackageManager
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CameraIDReader(
    onIDDetected: (String?) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    // Request permissions dynamically
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                context as android.app.Activity,
                arrayOf(Manifest.permission.CAMERA),
                1001
            )
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { context ->
                previewView = PreviewView(context)
                previewView!!
            },
            modifier = Modifier.fillMaxSize()
        )

        LaunchedEffect(previewView) {
            if (previewView != null) {
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        setupCamera(
                            cameraProvider = cameraProvider,
                            lifecycleOwner = lifecycleOwner,
                            executor = executor,
                            previewView = previewView!!,
                            onIDDetected = onIDDetected,
                            onError = onError
                        )
                    } catch (e: Exception) {
                        onError("Failed to initialize camera: ${e.message}")
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        }
    }
}

private fun setupCamera(
    cameraProvider: ProcessCameraProvider,
    lifecycleOwner: LifecycleOwner,
    executor: ExecutorService,
    previewView: PreviewView,
    onIDDetected: (String?) -> Unit,
    onError: (String) -> Unit
) {
    try {
        val preview = Preview.Builder().build().apply {
            setSurfaceProvider(previewView.surfaceProvider)
        }

        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build().apply {
                setAnalyzer(executor) { imageProxy ->
                    analyzeImage(imageProxy, onIDDetected, onError)
                }
            }

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        // Unbind all and rebind the camera
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageAnalyzer
        )
    } catch (e: Exception) {
        onError("Error setting up camera: ${e.message}")
    }
}

@OptIn(ExperimentalGetImage::class)
private fun analyzeImage(
    imageProxy: ImageProxy,
    onIDDetected: (String?) -> Unit,
    onError: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val detectedID = extractAnimalIdFromText(visionText)
                if (detectedID != null) {
                    onIDDetected(detectedID) // Pass detected ID
                }
            }
            .addOnFailureListener { e ->
                onError("Failed to process image: ${e.message}")
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}


private fun extractAnimalIdFromText(visionText: Text): String? {
    // Normalize the text by removing extra spaces and line breaks
    val normalizedText = visionText.text.replace("\\s+".toRegex(), "")

    // Updated regex to capture "RO" followed by 4 digits and then 6 digits
    val regex = Regex("RO\\d{10}")

    // Match the pattern in the normalized text
    val match = regex.find(normalizedText)

    // Return the matched value if found, otherwise return null
    return match?.value
}
