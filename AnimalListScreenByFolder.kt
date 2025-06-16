package com.example.farmerappfrontend

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
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
fun AnimalListScreenByFolder(
    token: String,
    folderId: String,
    navController: NavController,
    ownerId: String
) {
    var animals by remember { mutableStateOf<List<Animal>>(emptyList()) }
    var filteredAnimals by remember { mutableStateOf<List<Animal>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedAnimals by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isCounting by remember { mutableStateOf(false) }
    var popupMessage by remember { mutableStateOf<String?>(null) }
    var sortOrder by remember { mutableStateOf<SortOrder>(SortOrder.NONE) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(folderId) {
        fetchAnimalsInFolder(folderId, token) {
            animals = it
            filteredAnimals = it
        }
    }

    LaunchedEffect(searchQuery, sortOrder) {
        filteredAnimals = if (searchQuery.isEmpty()) {
            animals
        } else {
            animals.filter { animal ->
                animal.id.contains(searchQuery, ignoreCase = true) ||
                        animal.birthDate.contains(searchQuery, ignoreCase = true)
            }
        }

        filteredAnimals = when (sortOrder) {
            SortOrder.BIRTHDATE_ASC -> filteredAnimals.sortedBy { it.birthDate }
            SortOrder.BIRTHDATE_DESC -> filteredAnimals.sortedByDescending { it.birthDate }
            SortOrder.ID_ASC -> filteredAnimals.sortedBy { it.id }
            SortOrder.ID_DESC -> filteredAnimals.sortedByDescending { it.id }
            SortOrder.NONE -> filteredAnimals
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Animals in Folder") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { isCounting = true }) {
                        Icon(Icons.Default.Camera, "Camera")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(MaterialTheme.colorScheme.primary)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar with Sort Icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search animals...") },
                    leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = "Search") },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                )
                var expanded by remember { mutableStateOf(false) }
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Sort")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Sort by Birthdate (Oldest First)") },
                        onClick = {
                            sortOrder = SortOrder.BIRTHDATE_ASC
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Sort by Birthdate (Newest First)") },
                        onClick = {
                            sortOrder = SortOrder.BIRTHDATE_DESC
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Sort by ID (Ascending)") },
                        onClick = {
                            sortOrder = SortOrder.ID_ASC
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Sort by ID (Descending)") },
                        onClick = {
                            sortOrder = SortOrder.ID_DESC
                            expanded = false
                        }
                    )
                }
            }

            // Action Buttons (Select All and Delete)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { selectedAnimals = filteredAnimals.map { it.id }.toSet() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF66BB6A)), // Green
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Select All")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        scope.launch {
                            deleteSelectedAnimalsFromFolders(folderId, selectedAnimals.toList(), token) {
                                fetchAnimalsInFolder(folderId, token) {
                                    animals = it
                                    filteredAnimals = it
                                    selectedAnimals = emptySet()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)), // Red
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Delete")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Animal List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredAnimals) { animal ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedAnimals = if (selectedAnimals.contains(animal.id)) {
                                    selectedAnimals - animal.id
                                } else {
                                    selectedAnimals + animal.id
                                }
                            },
                        elevation = CardDefaults.elevatedCardElevation()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val genderSymbol = if (animal.gender.equals("male", ignoreCase = true)) "♂" else "♀"
                            Column {
                                Text("${animal.id} $genderSymbol", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = "Born: ${animal.birthDate}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (selectedAnimals.contains(animal.id)) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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
                    onClick = {
                        scope.launch {
                            try {
                                navController.navigate("notReadAnimals/$token/${animals.map { it.id }.joinToString(",")}/$ownerId?folderId=$folderId")
                            } catch (e: Exception) {
                                popupMessage = "Error: ${e.message}"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Show Not Read")
                }
            }
        }
    }

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

    if (popupMessage != null) {
        AlertDialog(
            onDismissRequest = { popupMessage = null },
            title = { Text("Info") },
            text = { Text(popupMessage ?: "") },
            confirmButton = {
                Button(onClick = { popupMessage = null }) {
                    Text("OK")
                }
            }
        )
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