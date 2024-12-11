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
import kotlinx.coroutines.CoroutineScope

@Composable
fun AnimalListScreen(token: String, userId: String) {
    var animals by remember { mutableStateOf<List<Animal>>(emptyList()) }
    var filteredAnimals by remember { mutableStateOf<List<Animal>>(emptyList()) }
    var selectedAnimals by remember { mutableStateOf<Set<String>>(emptySet()) }
    var errorMessage by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // Fetch the animals list
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val response = RetrofitClient.apiService.getAnimalsByOwnerId(userId, "Bearer $token")
                if (response.isSuccessful) {
                    animals = response.body() ?: emptyList()
                    filteredAnimals = animals
                } else {
                    errorMessage = "Error fetching animals: ${response.message()}"
                }
            } catch (e: Exception) {
                errorMessage = "Error fetching animals: ${e.message}"
            }
        }
    }

    // Filter animals based on search query
    LaunchedEffect(searchQuery) {
        filteredAnimals = if (searchQuery.isEmpty()) {
            animals
        } else {
            animals.filter { animal ->
                animal.id.contains(searchQuery, ignoreCase = true) || // Search by animal ID
                        animal.birthDate.contains(searchQuery, ignoreCase = true) // Search by birth date
            }
        }
    }

    // Screen Layout
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
            "Animal List",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Error Message
        if (errorMessage.isNotEmpty()) {
            Text(
                errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(8.dp)
            )
        }

        // Animal List
        if (filteredAnimals.isEmpty() && errorMessage.isEmpty()) {
            // Loading/Empty State
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // Display Animal Rows
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredAnimals) { animal ->
                    AnimalRow(animal, selectedAnimals) { animalId ->
                        // Toggle the selected state of the animal
                        selectedAnimals = if (selectedAnimals.contains(animalId)) {
                            selectedAnimals - animalId
                        } else {
                            selectedAnimals + animalId
                        }
                    }
                }
            }

            // Delete Button (only visible if any animals are selected)
            if (selectedAnimals.isNotEmpty()) {
                Button(
                    onClick = {
                        deleteSelectedAnimals(selectedAnimals, token, scope)
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Text("Delete Selected Animals")
                }
            }
        }
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

fun deleteSelectedAnimals(selectedAnimals: Set<String>, token: String, scope: CoroutineScope) {
    scope.launch {
        // Iterate through the selected animals and delete each one
        for (animalId in selectedAnimals) {
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

