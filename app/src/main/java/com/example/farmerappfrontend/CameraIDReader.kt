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
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope

@Composable
fun CameraIDReader(
    onIDDetected: (String?) -> Unit,
    onPartialIdDetected: (String) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var cooldownActive by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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
                            onPartialIdDetected = onPartialIdDetected,
                            onError = onError,
                            cooldownActive = cooldownActive,
                            setCooldownActive = { cooldownActive = it },
                            scope = scope
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
    onPartialIdDetected: (String) -> Unit,
    onError: (String) -> Unit,
    cooldownActive: Boolean,
    setCooldownActive: (Boolean) -> Unit,
    scope: CoroutineScope
) {
    try {
        val preview = Preview.Builder().build().apply {
            setSurfaceProvider(previewView.surfaceProvider)
        }
        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build().apply {
                setAnalyzer(executor) { imageProxy ->
                    analyzeImage(
                        imageProxy,
                        onIDDetected,
                        onPartialIdDetected,
                        onError
                    )
                }
            }
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
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
    onPartialIdDetected: (String) -> Unit,
    onError: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastReadTime.get() < 5000) {
            imageProxy.close()
            return
        }
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val detectedID = extractAnimalIdFromText(visionText)
                if (detectedID != null) {
                    if (detectedID.length == 12) {
                        onIDDetected(detectedID)
                    } else if (detectedID.length in 7..11) {
                        onPartialIdDetected(detectedID)
                    }
                    lastReadTime.set(System.currentTimeMillis())
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

// Add a static variable outside the function to hold the last read time
private val lastReadTime = java.util.concurrent.atomic.AtomicLong(0L)

private fun extractAnimalIdFromText(visionText: Text): String? {
    val normalizedText = visionText.text.replace("\\s+".toRegex(), "")
    val regex = Regex("RO\\d{5,12}")
    val match = regex.find(normalizedText)
    return match?.value
}
