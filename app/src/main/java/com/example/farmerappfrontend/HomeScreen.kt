// Updated HomeScreen.kt
package com.example.farmerappfrontend

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController


@Composable
fun HomeScreen(
    token: String,
    userId: String, // Pass userId as a parameter
    onLogout: () -> Unit,
    navController: NavController
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Welcome Text
        Text("Welcome to the Farmer App", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // Camera Button
        Button(onClick = { navController.navigate("camera") }) {
            Text("Camera")
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Animals List Button (Now passing userId and token)
        Button(onClick = { navController.navigate("animals/$userId") }) {
            Text("Animal List")
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Files Button
        Button(onClick = { navController.navigate("files") }) {
            Text("Files")
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Logout Button
        Button(onClick = { onLogout() }) {
            Text("Logout")
        }
    }
}
