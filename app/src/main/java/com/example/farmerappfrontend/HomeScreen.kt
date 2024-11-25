package com.example.farmerappfrontend

// HomeScreen.kt


import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import kotlinx.coroutines.launch

@Composable
fun HomeScreen(token: String, onLogout: () -> Unit) {
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    LaunchedEffect(token) {
        scope.launch {
            try {
                val response = RetrofitClient.apiService.getUserProfile("Bearer $token")
                userProfile = response
            } catch (e: Exception) {
                errorMessage = "Failed to load profile: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator()
        }

        userProfile?.let {
            Text("Welcome, ${it.name}")
            Spacer(modifier = Modifier.height(8.dp))
            Text("ID: ${it.id}")
            Text("Date of Birth: ${it.dob}")
            Text("Address: ${it.address}")
        }

        errorMessage.takeIf { it.isNotEmpty() }?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Logout button
        Button(onClick = { onLogout() }) {
            Text("Logout")
        }
    }
}







