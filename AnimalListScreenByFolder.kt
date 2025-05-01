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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimalListScreenByFolder(token: String, folderId: String, navController: NavController,ownerId:String) {
    var animals by remember { mutableStateOf<List<Animal>>(emptyList()) }
    var filteredAnimals by remember { mutableStateOf<List<Animal>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedAnimals by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isCounting by remember { mutableStateOf(false) }
    var isCameraOpen by remember { mutableStateOf(false) }
    var popupMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var isCooldownActive by remember { mutableStateOf(false) }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Animals in Folder") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(MaterialTheme.colorScheme.primary)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search animals...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = MaterialTheme.shapes.medium,
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Check, contentDescription = "Search")
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { isCounting = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Count")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { isCameraOpen = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Add via Camera")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text("Delete Selected")
                }

                Spacer(modifier = Modifier.height(8.dp))
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f), // Ensure it takes the available space
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredAnimals.filterNotNull()) { animal ->
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

    // Counting Camera
    if (isCounting) {
        CountingCamera(
            token = token,
            animalIds = animals.map { it.id },
            onComplete = { idsNotRead ->
                isCounting = false
                navController.navigate("notReadAnimals/$token/${idsNotRead.joinToString(",")}/$ownerId?folderId=$folderId")
            }
        )
    }

    // Camera ID Reader
    if (isCameraOpen) {
        CameraIDReader(
            onIDDetected = { scannedId ->
                if (scannedId != null && !isCooldownActive) { // Check cooldown
                    isCooldownActive = true // Start cooldown
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

                        kotlinx.coroutines.delay(5000) // Cooldown duration
                        isCooldownActive = false // Reset cooldown
                    }
                }
            },
            onError = { error ->
                popupMessage = "Error: $error"
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun AnimalRowWithSelection(
    animal: Animal,
    isSelected: Boolean,
    onToggleSelection: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onToggleSelection(animal.id) },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = if (isSelected) {
            CardDefaults.cardColors(containerColor = Color(0xFF90CAF9)) // Highlight selected
        } else {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
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
}

fun fetchAnimalsInFolder(folderId: String, token: String, onResult: (List<Animal>) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = RetrofitClient.apiService.getAnimalsByFolderId(folderId, "Bearer $token")
            if (response.isSuccessful) {
                onResult(response.body()?.filterNotNull() ?: emptyList())
            } else {
                onResult(emptyList()) // Handle failure gracefully
            }
        } catch (e: Exception) {
            onResult(emptyList()) // Handle exception gracefully
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
                Log.d("DeleteAnimals", "Animals successfully removed from folder: $folderId")
                onComplete() // Notify the caller on success
            } else {
                Log.e("DeleteAnimals", "Failed to remove animals: ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("DeleteAnimals", "Error removing animals from folder: ${e.message}")
        }
    }
}