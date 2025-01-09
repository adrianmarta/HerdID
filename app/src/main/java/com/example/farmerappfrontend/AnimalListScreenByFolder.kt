package com.example.farmerappfrontend

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun AnimalListScreenByFolder(token: String, folderId: String, navController: NavController) {
    var animals by remember { mutableStateOf<List<Animal>>(emptyList()) }
    var filteredAnimals by remember { mutableStateOf<List<Animal>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedAnimals by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isCounting by remember { mutableStateOf(false) }
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

    if (isCounting) {
        CountingCamera(
            token = token,
            animalIds = animals.map { it.id },
            onComplete = { idsNotRead ->
                isCounting = false
                navController.navigate("notReadAnimals/$token/${idsNotRead.joinToString(",")}/$folderId?folderId=$folderId")
            }
        )
    }
    else if (isCameraOpen) {
        CameraIDReader(
            onIDDetected = { scannedId ->
                if (scannedId != null) {
                    scope.launch {
                        handleScannedId(
                            token = token,
                            folderId = folderId,
                            scannedId = scannedId,
                            currentAnimals = animals,
                            onResult = { result ->
                                popupMessage = result
                            }
                        )

                        fetchAnimalsInFolder(folderId, token) { updatedAnimals ->
                            animals = updatedAnimals
                            filteredAnimals = updatedAnimals
                        }
                    }
                }
            },
            onError = { error ->
                popupMessage = "Error: $error"
            },
            modifier = Modifier.fillMaxSize()
        )
    } else {
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
                color = MaterialTheme.colorScheme.primary
            )

            // Count Button
            Button(
                onClick = { isCounting = true },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Text("Count Animals")
            }

            // Add Using Camera Button
            Button(
                onClick = { isCameraOpen = true },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Text("Add Animal using Camera")
            }

            // Delete Button
            if (selectedAnimals.isNotEmpty()) {
                Button(
                    onClick = {
                        scope.launch {
                            deleteSelectedAnimalsFromFolders(
                                folderId = folderId,
                                animalIds = selectedAnimals.toList(),
                                token = token
                            ) {
                                fetchAnimalsInFolder(folderId, token) { updatedAnimals ->
                                    animals = updatedAnimals
                                    filteredAnimals = updatedAnimals
                                    selectedAnimals = emptySet()
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Text("Delete Selected Animals")
                }
            }

            // Animal List
            if (filteredAnimals.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No animals in this folder.", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredAnimals) { animal ->
                        AnimalRowWithSelection(
                            animal = animal,
                            isSelected = selectedAnimals.contains(animal.id),
                            onToggleSelection = { animalId ->
                                selectedAnimals = if (selectedAnimals.contains(animalId)) {
                                    selectedAnimals - animalId
                                } else {
                                    selectedAnimals + animalId
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Popup Message
    popupMessage?.let {
        AlertDialog(
            onDismissRequest = { popupMessage = null },
            title = { Text("Animal Action") },
            text = { Text(it) },
            confirmButton = {
                Button(onClick = { popupMessage = null }) {
                    Text("OK")
                }
            }
        )
    }
}


@Composable
fun AnimalRowWithSelection(
    animal: Animal,
    isSelected: Boolean,
    onToggleSelection: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onToggleSelection(animal.id) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val genderSymbol = if (animal.gender.equals("male", ignoreCase = true)) "♂" else "♀"
        Text(
            text = "${animal.id}   $genderSymbol   ${animal.birthDate}",
            style = MaterialTheme.typography.bodyLarge
        )
        if (isSelected) {
            Icon(imageVector = Icons.Default.Check, contentDescription = "Selected")
        }
    }
}


// Function to delete selected animals from a folder
fun deleteSelectedAnimalsFromFolders(
    folderId: String,
    animalIds: List<String>,
    token: String,
    onComplete: () -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = RetrofitClient.apiService.removeAnimalsFromFolder(folderId, animalIds, "Bearer $token")
            if (response.isSuccessful) {
                onComplete()
            }
        } catch (e: Exception) {
            Log.e("Delete", "Error deleting animals from folder: ${e.message}")
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
            onResult("Failed to add animal to folder: ${addResponse.message()} (Code: ${addResponse.code()})")
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
            } else {
                onResult(emptyList()) // Handle failure gracefully
            }
        } catch (e: Exception) {
            onResult(emptyList()) // Handle exception gracefully
        }
    }
}



