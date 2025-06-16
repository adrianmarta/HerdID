package com.example.farmerappfrontend

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimalListScreen(token: String, userId: String, navController: NavController) {
    val scope = rememberCoroutineScope()
    var animals by remember { mutableStateOf<List<Animal>>(emptyList()) }
    var filteredAnimals by remember { mutableStateOf<List<Animal>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedAnimals by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isCounting by remember { mutableStateOf(false) }

    // Fetch animals on component load
    LaunchedEffect(Unit) {
        loadAnimals(token, userId) { result ->
            animals = result
            filteredAnimals = result
        }
    }

    // Filter animals when the search query changes
    LaunchedEffect(searchQuery) {
        filteredAnimals = if (searchQuery.isEmpty()) {
            animals
        } else {
            animals.filter { it.id.contains(searchQuery, ignoreCase = true) || it.birthDate.contains(searchQuery, ignoreCase = true) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HerdID") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(MaterialTheme.colorScheme.primary)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search animals...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                leadingIcon = {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Search")
                },
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons (Select All and Deces)
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
                            deleteSelectedAnimals(selectedAnimals, token)
                            // Reload animals after deletion
                            loadAnimals(token, userId) { result ->
                                animals = result
                                filteredAnimals = result
                                selectedAnimals = emptySet()
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

            // Animal List (Scrollable)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), // Makes LazyColumn fill available space
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

            // Bottom Buttons (Fixed)
            // Bottom Buttons (Side by Side)
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

                Spacer(modifier = Modifier.width(8.dp)) // Space between the buttons

                Button(
                    onClick = { navController.navigate("fileUpload/$token") },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Upload Animal")
                }
            }

        }
    }

    // Show Counting Camera
    if (isCounting) {
        CountingCamera(
            token = token,
            animalIds = animals.map { it.id },
            onComplete = { idsNotRead ->
                isCounting = false
                navController.navigate("notReadAnimals/$token/${idsNotRead.joinToString(",")}/$userId")
            }
        )
    }
}

// Function to Delete Selected Animals
fun deleteSelectedAnimals(selectedAnimals: Set<String>, token: String) {
    CoroutineScope(Dispatchers.IO).launch {
        selectedAnimals.forEach { animalId ->
            try {
                val response = RetrofitClient.apiService.deleteAnimal(animalId, "Bearer $token")
                if (response.isSuccessful) {
                    Log.d("Delete", "Animal $animalId deleted successfully.")
                } else {
                    Log.e("Delete", "Failed to delete animal $animalId: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("Delete", "Error deleting animal $animalId: ${e.message}")
            }
        }
    }
}

// Function to Load Animals
fun loadAnimals(token: String, userId: String, onComplete: (List<Animal>) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = RetrofitClient.apiService.getAnimalsByOwnerId(userId, "Bearer $token")
            if (response.isSuccessful) {
                onComplete(response.body() ?: emptyList())
            } else {
                onComplete(emptyList())
                Log.e("LoadAnimals", "Error fetching animals: ${response.message()}")
            }
        } catch (e: Exception) {
            onComplete(emptyList())
            Log.e("LoadAnimals", "Error: ${e.message}")
        }
    }
}
