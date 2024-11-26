package com.example.farmerappfrontend

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun AnimalListScreen(token: String, userId: String) {
    var animals by remember { mutableStateOf<List<Animal>>(emptyList()) }
    var errorMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

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
