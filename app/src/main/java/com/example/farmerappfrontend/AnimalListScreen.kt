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
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.Dispatchers

@Composable
fun AnimalListScreen(token: String, userId: String, navController: NavController) {
    val scope = rememberCoroutineScope()
    var animals by remember { mutableStateOf<List<Animal>>(emptyList()) }
    var filteredAnimals by remember { mutableStateOf<List<Animal>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isCounting by remember { mutableStateOf(false) }
    var selectedAnimals by remember { mutableStateOf<Set<String>>(emptySet()) }
     // Add navController for navigation

    // Fetch animals on component load
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val response = RetrofitClient.apiService.getAnimalsByOwnerId(userId, "Bearer $token")
                if (response.isSuccessful) {
                    animals = response.body() ?: emptyList()
                    filteredAnimals = animals // Set initial filter
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    // Filter animals when the search query changes
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

    // Layout
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
            "Animals List",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        // Count Button
        Button(
            onClick = {
                isCounting = true
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Text("Count Animals")
        }

        // Animal List
        LazyColumn(
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredAnimals) { animal ->
                AnimalRow(
                    animal = animal,
                    selectedAnimals = selectedAnimals,
                    onAnimalClick = { animalId ->
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

    // Show Counting Camera and Navigate to NotReadAnimalsScreen with IDs
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



@Composable
fun AnimalRow(animal: Animal, selectedAnimals: Set<String>, onAnimalClick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable { onAnimalClick(animal.id) }, // Toggle selection on click
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val genderSymbol = if (animal.gender.equals("male", ignoreCase = true)) "♂" else "♀"
        Text(
            text = "${animal.id}   $genderSymbol   ${animal.birthDate}",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        // Display check mark if the animal is selected
        if (selectedAnimals.contains(animal.id)) {
            Icon(imageVector = Icons.Default.Check, contentDescription = "Selected")
        }
    }
}

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
