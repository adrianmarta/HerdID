package com.example.farmerappfrontend

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import androidx.compose.foundation.layout.*

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.farmerappfrontend.ui.theme.FarmerAppFrontendTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApp()
        }
    }
}

// MainActivity.kt
@Composable
fun MyApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val token = TokenManager.getToken(context)

    NavHost(
        navController = navController,
        startDestination = if (token != null) "home/$token" else "login"
    ) {
        composable("login") {
            LoginScreen(onLoginSuccess = { newToken ->
                TokenManager.saveToken(context, newToken)
                navController.navigate("home/$newToken") {
                    popUpTo("login") { inclusive = true }
                }
            })
        }
        composable("home/{token}") { backStackEntry ->
            val token = backStackEntry.arguments?.getString("token") ?: ""
            var userId by remember { mutableStateOf<String?>(null) }
            var errorMessage by remember { mutableStateOf<String?>(null) }

            // Fetch the userId from the server or decode it from the token
            LaunchedEffect(token) {
                try {
                    val userProfile = RetrofitClient.apiService.getUserProfile("Bearer $token") // Adjust this call if needed
                    userId = userProfile.id // Assuming the API returns a `UserProfile` with an `id` field
                } catch (e: Exception) {
                    errorMessage = "Failed to fetch user ID: ${e.message}"
                }
            }

            if (userId != null) {
                HomeScreen(
                    token = token,
                    userId = userId!!, // Pass the fetched userId to HomeScreen
                    onLogout = {
                        TokenManager.removeToken(context)
                        navController.popBackStack("login", false)
                    },
                    navController = navController
                )
            } else {
                // Show a loading or error state while fetching the userId
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (errorMessage != null) {
                        Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                    } else {
                        CircularProgressIndicator()
                    }
                }
            }
        }

        composable("animals/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            if (token != null) {
                AnimalListScreen(
                    token = token,  // You still need to pass the token here
                    userId = userId
                )
            }

        }
        composable("camera") { CameraScreen() }
        composable("files") { FilesScreen() }
    }

}







@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    FarmerAppFrontendTheme {
        MyApp()
    }
}
