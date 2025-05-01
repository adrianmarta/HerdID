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
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

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

    var isValidUser by remember { mutableStateOf(false) }
    var isCheckingValidity by remember { mutableStateOf(true) }

    // Validate token and user profile
    LaunchedEffect(Unit) {
        if (token != null) {
            try {
                val userProfile = RetrofitClient.apiService.getUserProfile("Bearer $token")
                isValidUser = userProfile != null
            } catch (e: Exception) {
                isValidUser = false
            } finally {
                isCheckingValidity = false
            }
        } else {
            isValidUser = false
            isCheckingValidity = false
        }
    }

    // Show loading indicator while validating
    if (isCheckingValidity) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        NavHost(
            navController = navController,
            startDestination = if (isValidUser) "home/$token" else "login"
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

                LaunchedEffect(token) {
                    try {
                        val userProfile = RetrofitClient.apiService.getUserProfile("Bearer $token")
                        userId = userProfile.id
                    } catch (e: Exception) {
                        errorMessage = "Failed to fetch user ID: ${e.message}"
                    }
                }

                if (userId != null) {
                    HomeScreen(
                        token = token,
                        userId = userId!!,
                        onLogout = {
                            TokenManager.removeToken(context)
                            navController.navigate("login") {
                                popUpTo("home") { inclusive = true }
                            }
                        },
                        navController = navController
                    )
                } else {
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
                    AnimalListScreen(token = token, userId = userId, navController = navController)
                }
            }

            composable("camera/{token}") { backStackEntry ->
                val token = backStackEntry.arguments?.getString("token") ?: ""
                CameraScreen(token = token) // Pass the token here
            }
            composable("files") {
                FilesScreen(navController)
            }
            composable("folder/{folderName}") { backStackEntry ->
                val folderName = backStackEntry.arguments?.getString("folderName") ?: ""
                if (token != null) {
                    AnimalListScreen(
                        token = token,
                        userId = folderName,
                        navController = navController // Here, the folder name acts as an identifier for animals in the folder
                    )
                }
            }
            composable("folder/{folderId}/animals") { backStackEntry ->
                val folderId = backStackEntry.arguments?.getString("folderId") ?: ""
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                if (token != null) {
                    AnimalListScreenByFolder(
                        token = token,
                        folderId = folderId,
                        navController=navController,
                        ownerId = userId
                    )
                }
            }
            composable("countAnimals/{token}/{ownerId}") { backStackEntry ->
                val token = backStackEntry.arguments?.getString("token") ?: ""
                val ownerId = backStackEntry.arguments?.getString("ownerId") ?: ""
                var animalIds by remember { mutableStateOf<List<String>>(emptyList()) }
                var isLoading by remember { mutableStateOf(true) }
                val scope = rememberCoroutineScope()

                LaunchedEffect(Unit) {
                    scope.launch {
                        try {
                            val response = RetrofitClient.apiService.getAnimalsByOwnerId(ownerId, "Bearer $token")
                            if (response.isSuccessful) {
                                animalIds = response.body()?.map { it.id } ?: emptyList()
                            }
                        } catch (e: Exception) {
                            // Handle error (e.g., show a message or log the error)
                        } finally {
                            isLoading = false
                        }
                    }
                }

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    CountingCamera(
                        token = token,
                        animalIds = animalIds,
                        onComplete = { notReadIds ->
                            navController.navigate(
                                "notReadAnimals/$token/${notReadIds.joinToString(",")}/$ownerId"
                            ) {
                                popUpTo("countAnimals/$token/$ownerId") { inclusive = true }
                            }
                        }
                    )
                }
            }


            composable("notReadAnimals/{token}/{notReadIds}/{ownerId}?folderId={folderId}") { backStackEntry ->
                val token = backStackEntry.arguments?.getString("token") ?: ""
                val notReadIdsString = backStackEntry.arguments?.getString("notReadIds") ?: ""
                val notReadIds = notReadIdsString.split(",")
                val ownerId = backStackEntry.arguments?.getString("ownerId") ?: ""
                val folderId = backStackEntry.arguments?.getString("folderId") // Extract folderId if present

                NotReadAnimalsScreen(
                    token = token,
                    animalIds = notReadIds,
                    ownerId = ownerId,
                    onNavigateBack = { navController.popBackStack() },
                    folderId = folderId // Pass folderId to NotReadAnimalsScreen
                )
            }





        }
    }
}




@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    FarmerAppFrontendTheme {
        MyApp()
    }
}