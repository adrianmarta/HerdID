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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    token: String,
    userId: String,
    onLogout: () -> Unit,
    navController: NavController
) {
    val customPurple = Color(0xFF6650a4) // Primary color for branding

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "HerdID",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                },
                actions = {
                    Button(
                        onClick = {
                            onLogout()
                            navController.navigate("login") {
                                popUpTo("home") { inclusive = true }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(horizontal = 16.dp) // Add padding to fit the text properly
                    ) {
                        Text(
                            "Log Out",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = customPurple,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background // Ensure a consistent background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Camera Button
            Button(
                onClick = { navController.navigate("camera/$token") },
                colors = ButtonDefaults.buttonColors(containerColor = customPurple),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .width(250.dp)
                    .padding(vertical = 12.dp)
            ) {
                Text("Camera", fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Animal List Button
            Button(
                onClick = { navController.navigate("animals/$userId") },
                colors = ButtonDefaults.buttonColors(containerColor = customPurple),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .width(250.dp)
                    .padding(vertical = 12.dp)
            ) {
                Text("Animal List", fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Files Button
            Button(
                onClick = { navController.navigate("files") },
                colors = ButtonDefaults.buttonColors(containerColor = customPurple),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .width(250.dp)
                    .padding(vertical = 12.dp)
            ) {
                Text("Files", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

