package com.example.farmerappfrontend

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    token: String,
    onLogout: () -> Unit,
    navController: NavController
) {
    val scope = rememberCoroutineScope()
    val customPurple = Color(0xFF6650a4)
    val buttons = listOf(
        ButtonData("Camera", Icons.Default.CameraAlt, Color(0xFF6650a4)) {
            scope.launch {
                try {
                    val response = RetrofitClient.apiService.getAnimalsByOwnerId("Bearer $token")
                    if (response.isSuccessful) {
                        navController.navigate("countAnimals/$token")
                    }
                } catch (_: Exception) {}
            }
        },
        ButtonData("Animal List", Icons.Default.List, Color(0xFF009688)) { navController.navigate("animals/$token") },
        ButtonData("Files", Icons.Default.Folder, Color(0xFFFFA726)) { navController.navigate("files/$token") },
        ButtonData("Statistics", Icons.Default.BarChart, Color(0xFF1976D2)) { navController.navigate("statistics/$token") },
        ButtonData("Sessions", Icons.Default.History, Color(0xFFE53935)) { navController.navigate("sessions/$token") },
        ButtonData("Transfers", Icons.Default.SwapHoriz, Color(0xFFFFEB3B)) { navController.navigate("transfers/$token") }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "HerdID Logo",
                        modifier = Modifier.size(40.dp)
                    )
                },
                actions = {
                    Button(onClick = {
                        onLogout(); navController.navigate("login") { popUpTo("home") { inclusive = true } }
                    }, colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)) {
                        Text("Log Out", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = customPurple)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(Color(0xFFF3EFFF), Color.White))
                ),
            contentAlignment = Alignment.Center
        ) {
            

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxWidth(0.8f)
            ) {
                items(buttons) { data ->
                    HomeGridButton(
                        label = data.label,
                        icon = data.icon,
                        color = data.color,
                        onClick = data.onClick,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}

data class ButtonData(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

@Composable
fun HomeGridButton(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = label, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
    }
}
