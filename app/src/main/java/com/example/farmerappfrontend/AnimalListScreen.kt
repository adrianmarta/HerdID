package com.example.farmerappfrontend

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment

@Composable
fun AnimalListScreen(token: String, userId: String) {
    var animals by remember { mutableStateOf<List<Animal>>(emptyList()) }
    var errorMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // Fetch the animals list
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val response = RetrofitClient.apiService.getAnimalsByOwnerId(userId, "Bearer $token")
                if (response.isSuccessful) {
                    animals = response.body() ?: emptyList()
                } else {
                    errorMessage = "Error fetching animals: ${response.message()}"
                }
            } catch (e: Exception) {
                errorMessage = "Error fetching animals: ${e.message}"
            }
        }
    }

    // Screen Layout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
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
        if (animals.isEmpty() && errorMessage.isEmpty()) {
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
                items(animals) { animal ->
                    AnimalRow(animal)
                }
            }
        }
    }
}

@Composable
fun AnimalRow(animal: Animal) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Combine all text into one line
        val genderSymbol = if (animal.gender.equals("male", ignoreCase = true)) "♂" else "♀"
        Text(
            text = "${animal.id}   $genderSymbol   ${animal.birthDate}", // All on one line
            style = MaterialTheme.typography.titleLarge, // Larger text
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

