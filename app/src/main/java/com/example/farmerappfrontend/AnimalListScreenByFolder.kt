package com.example.farmerappfrontend

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun AnimalListScreenByFolder(token: String, folderId: String) {
    var animals by remember { mutableStateOf<List<Animal>>(emptyList()) }
    var filteredAnimals by remember { mutableStateOf<List<Animal>>(emptyList()) }
    var errorMessage by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var isCameraOpen by remember { mutableStateOf(false) }
    var popupMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Fetch animals in the folder
    LaunchedEffect(folderId) {
        fetchAnimalsInFolder(folderId, token) {
            animals = it
            filteredAnimals = it
        }
    }

    // Filter animals based on search query
    LaunchedEffect(searchQuery) {
        filteredAnimals = if (searchQuery.isEmpty()) {
            animals
        } else {
            animals.filter { animal ->
                animal.id.contains(searchQuery, ignoreCase = true) ||
                        animal.birthDate.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search Bar
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search Animals") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )

        // Title
        Text(
            "Animals in Folder",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        )

        // Add Animal Button
        Button(
            onClick = {
                isCameraOpen = true // Update the state to open the camera
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Text("Add Animal using Camera")
        }

        // Error Message
        if (errorMessage.isNotEmpty()) {
            Text(
                errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(8.dp)
            )
        }

        // Animal List or Empty State
        if (filteredAnimals.isEmpty() && errorMessage.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No animals yet.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredAnimals) { animal ->
                    AnimalRow(animal, emptySet()) { /* No selection functionality needed */ }
                }
            }
        }

        // Camera Screen for Adding Animals
        if (isCameraOpen) {
            CameraIDReader(
                onIDDetected = { scannedId ->
                    isCameraOpen = false // Close the camera after scanning
                    if (scannedId != null) {
                        scope.launch {
                            handleScannedId(
                                token = token,
                                folderId = folderId,
                                scannedId = scannedId,
                                currentAnimals = animals,
                                onResult = { message ->
                                    popupMessage = message
                                    fetchAnimalsInFolder(folderId, token) { updatedAnimals ->
                                        animals = updatedAnimals
                                        filteredAnimals = updatedAnimals
                                    }
                                }
                            )
                        }
                    } else {
                        popupMessage = "No ID detected."
                    }
                },
                onError = { error ->
                    popupMessage = error
                    isCameraOpen = false // Close the camera on error
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Popup Message
        popupMessage?.let {
            LaunchedEffect(it) {
                kotlinx.coroutines.delay(5000) // Dismiss after 5 seconds
                popupMessage = null
            }
            AlertDialog(
                onDismissRequest = { popupMessage = null },
                title = { Text("Animal Addition") },
                text = { Text(it) },
                confirmButton = {}
            )
        }
    }
}

suspend fun handleScannedId(
    token: String,
    folderId: String,
    scannedId: String,
    currentAnimals: List<Animal>,
    onResult: (String) -> Unit
) {
    try {
        // Check if the scanned ID exists in the global list
        val globalResponse = RetrofitClient.apiService.checkAnimalExists(scannedId, "Bearer $token")
        if (!globalResponse.isSuccessful || globalResponse.body() != true) {
            onResult("The ID is not present in the global animal list.")
            return
        }

        // Check if the animal is already in the folder
        if (currentAnimals.any { it.id == scannedId }) {
            onResult("This animal is already in the folder.")
            return
        }

        // Add the animal to the folder
        val addResponse = RetrofitClient.apiService.addAnimalToFolder(folderId, scannedId, "Bearer $token")
        if (addResponse.isSuccessful) {
            onResult("Animal added successfully.")
        } else {
            onResult("Failed to add animal to folder: ${addResponse.message()}")
        }
    } catch (e: Exception) {
        onResult("Error: ${e.message}")
    }
}

fun fetchAnimalsInFolder(folderId: String, token: String, onResult: (List<Animal>) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = RetrofitClient.apiService.getAnimalsByFolderId(folderId, "Bearer $token")
            if (response.isSuccessful) {
                onResult(response.body() ?: emptyList())
            }
        } catch (e: Exception) {
            onResult(emptyList())
        }
    }
}


