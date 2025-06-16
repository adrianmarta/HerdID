package com.example.farmerappfrontend

import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    token: String,
    onLogout: () -> Unit,
    navController: NavController
) {
    val scope = rememberCoroutineScope()
    val customPurple = Color(0xFF6650a4) // Primary color for branding
    val cameraColor = Color(0xFF6650a4)
    val animalListColor = Color(0xFF009688)
    val filesColor = Color(0xFFFF9800)
    val statsColor = Color(0xFF1976D2)
    val context = LocalContext.current
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
                        contentPadding = PaddingValues(horizontal = 16.dp)
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
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF3EFFF), // light purple
                            Color(0xFFFFFFFF)  // white
                        )
                    )
                )
        ) {
            // Faint logo in the background, centered
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Background Logo",
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .alpha(0.08f)
            )
            // Main content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(48.dp))
                // 2x2 Grid of Main Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    verticalArrangement = Arrangement.spacedBy(60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(32.dp)
                    ) {
                        HomeGridButton(
                            label = "Camera",
                            icon = Icons.Default.CameraAlt,
                            color = cameraColor,
                            onClick = {
                                navController.navigate("countAnimals/$token")
                            },
                            modifier = Modifier.weight(1f)
                        )
                        HomeGridButton(
                            label = "Animal List",
                            icon = Icons.Default.List,
                            color = animalListColor,
                            onClick = { navController.navigate("animals") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(32.dp)
                    ) {
                        HomeGridButton(
                            label = "Files",
                            icon = Icons.Default.Folder,
                            color = filesColor,
                            onClick = { navController.navigate("files") },
                            modifier = Modifier.weight(1f)
                        )
                        HomeGridButton(
                            label = "Statistics",
                            icon = Icons.Default.BarChart,
                            color = statsColor,
                            onClick = { navController.navigate("statistics") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeGridButton(label: String, icon: ImageVector, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .aspectRatio(1.6f)
            .fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
        }
    }
}

