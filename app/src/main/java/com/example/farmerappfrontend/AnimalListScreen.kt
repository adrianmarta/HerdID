// AnimalListScreen.kt
package com.example.farmerappfrontend

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun AnimalListScreen(token: String) {
    var animals by remember { mutableStateOf<List<Animal>>(emptyList()) }
    var errorMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                animals = RetrofitClient.apiService.getAllAnimals("Bearer $token")
            } catch (e: Exception) {
                errorMessage = "Error fetching animals: ${e.message}"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Animal List", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error)
        }

        LazyColumn {
            items(animals) { animal ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text("ID: ${animal.id}", Modifier.weight(1f))
                    Text("Gender: ${animal.gender}", Modifier.weight(1f))
                    Text("Birth Date: ${animal.birthDate}", Modifier.weight(1f))
                }
            }
        }
    }
}

// Animal data class
data class Animal(val id: String, val gender: String, val birthDate: String)
