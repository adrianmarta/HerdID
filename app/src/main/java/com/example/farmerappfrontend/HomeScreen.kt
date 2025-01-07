package com.example.farmerappfrontend

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun HomeScreen(
    token: String,
    userId: String, // Pass userId as a parameter
    onLogout: () -> Unit,
    navController: NavController
) {
    // Define the new purple color for both the header and the buttons
    val customPurple = Color(0xFF6650a4) // Updated color

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp), // Remove padding from the outer column to avoid extra space
    ) {
        // Header with HerdID and Logout button, no space between the top and header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(customPurple) // Set the background color for the header
                .padding(16.dp) // Padding inside the header to create space around the content
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bigger Logout Button
                IconButton(
                    onClick = { onLogout()
                        navController.navigate("login") { // Navigate to the login screen
                            popUpTo("home") { inclusive = true }
                        }
                    },
                    modifier = Modifier
                        .size(56.dp) // Bigger button size
                        .background(customPurple, shape = MaterialTheme.shapes.medium) // Rounded button
                        .border(0.dp, Color.Transparent) // Ensures no border around logout button
                ) {
                    Text(
                        "LogOut",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp // Increase font size for better visibility
                    )
                }

                // HerdID Logo
                Text(
                    "HerdID",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
            }
        }

        // Space between the header and the buttons
        Spacer(modifier = Modifier.height(16.dp))

        // Center the remaining buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f), // Take up available space to center the buttons
            horizontalAlignment = Alignment.CenterHorizontally, // Center the buttons horizontally
            verticalArrangement = Arrangement.Center // Center vertically within the available space
        ) {
            // Camera Button
            Button(onClick = { navController.navigate("camera/$token")  },
                modifier = Modifier
                    .width(250.dp) // Increased button width
                    .padding(vertical = 12.dp) // Vertical padding for spacing
                    .background(customPurple, shape = MaterialTheme.shapes.medium) // Rounded corners
                    .border(0.dp, Color.Transparent) // Ensure no border
            ) {
                Text("Camera", fontWeight = FontWeight.Bold, color = Color.White)
            }

            // Animal List Button
            Button(
                onClick = { navController.navigate("animals/$userId") },
                modifier = Modifier
                    .width(250.dp) // Increased button width
                    .padding(vertical = 12.dp) // Vertical padding for spacing
                    .background(customPurple, shape = MaterialTheme.shapes.medium) // Rounded corners
                    .border(0.dp, Color.Transparent) // Ensure no border
            ) {
                Text("Animal List", fontWeight = FontWeight.Bold, color = Color.White)
            }

            // Files Button
            Button(
                onClick = { navController.navigate("files") },
                modifier = Modifier
                    .width(250.dp) // Increased button width
                    .padding(vertical = 12.dp) // Vertical padding for spacing
                    .background(customPurple, shape = MaterialTheme.shapes.medium) // Rounded corners
                    .border(0.dp, Color.Transparent) // Ensure no border
            ) {
                Text("Files", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
