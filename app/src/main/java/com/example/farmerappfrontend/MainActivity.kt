package com.example.farmerappfrontend

import android.annotation.SuppressLint
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
@SuppressLint("NewApi")
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
                LoginScreen(
                    onLoginSuccess = { token ->
                        TokenManager.saveToken(context, token)
                        navController.navigate("home/$token") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    navToRegister = {
                        navController.navigate("register")
                    }
                )
            }

            composable("register") {
                RegisterScreen(
                    onRegisterSuccess = {
                        navController.navigate("login") {
                            popUpTo("register") { inclusive = true }
                        }
                    },
                    navToLogin = {
                        navController.navigate("login")
                    }
                )
            }

            composable("home/{token}") { backStackEntry ->
                val token = backStackEntry.arguments?.getString("token") ?: ""
                HomeScreen(
                    token = token,
                    onLogout = {
                        TokenManager.removeToken(context)
                        navController.navigate("login") {
                            popUpTo("home") { inclusive = true }
                        }
                    },
                    navController = navController
                )
            }
            composable("animals") { backStackEntry ->
                if (token != null) {
                    AnimalListScreen(token = token, navController = navController)
                }
            }

            composable("camera/{token}/{existingAnimalIds}") { backStackEntry ->
                val token = backStackEntry.arguments?.getString("token") ?: ""
                val existingAnimalIds = backStackEntry.arguments
                    ?.getString("existingAnimalIds")
                    ?.split(",") ?: emptyList()

                CameraScreen(
                    token = token,
                    navController = navController,
                    existingAnimalIds = existingAnimalIds
                )
            }
            composable("fileUpload/$token")
            {
                backStackEntry ->
                val token = backStackEntry.arguments?.getString("token") ?: ""
                AddAnimalsScreen(token=token,navController=navController)
            }
            composable("files") {
                FilesScreen(navController)
            }

            composable("folder/{folderId}/animals") { backStackEntry ->
                val folderId = backStackEntry.arguments?.getString("folderId") ?: ""

                if (token != null) {
                    AnimalListScreenByFolder(
                        token = token,
                        folderId = folderId,
                        navController=navController
                    )
                }
            }
            composable("countAnimals/{token}") { backStackEntry ->
                val token = backStackEntry.arguments?.getString("token") ?: ""
                var animalIds by remember { mutableStateOf<List<String>>(emptyList()) }
                var isLoading by remember { mutableStateOf(true) }

                LaunchedEffect(token) {
                    try {
                        val response = RetrofitClient.apiService.getAnimalsByOwnerId("Bearer $token")
                        if (response.isSuccessful) {
                            animalIds = response.body()?.map { it.id } ?: emptyList()
                            println("Fetched animal IDs: $animalIds")
                        } else {
                            println("Failed to fetch animals: ${response.message()}")
                        }
                    } catch (e: Exception) {
                        println("Error fetching animals: ${e.message}")
                    }
                    isLoading = false
                }

                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    CountingCamera(
                        token = token,
                        animalIds = animalIds,
                        onComplete = {
                            navController.navigate("home/$token") {
                                popUpTo("countAnimals/$token") { inclusive = true }
                            }
                        }
                    )
                }
            }

            composable("readAnimals/{token}/{readAnimalIds}/{newAnimalIds}") { backStackEntry ->
                val token = backStackEntry.arguments?.getString("token") ?: ""
                val readAnimalIds = backStackEntry.arguments?.getString("readAnimalIds")?.split(",") ?: emptyList()
                val newAnimalIds = backStackEntry.arguments?.getString("newAnimalIds")?.split(",") ?: emptyList()

                ReadAnimalsScreen(
                    token = token,
                    readAnimalIds = readAnimalIds,
                    newAnimalIds = newAnimalIds,
                    navController = navController
                )
            }



            composable("statistics") { backStackEntry ->
                StatisticsScreen(navController = navController)
            }

            composable("addSingleAnimal/{token}/{animalId}") { backStackEntry ->
                val token = backStackEntry.arguments?.getString("token") ?: ""
                val animalId = backStackEntry.arguments?.getString("animalId") ?: ""
                AddSingleAnimalScreen(token = token, animalId = animalId, navController = navController)
            }

            composable("animalDetails/{animalId}") { backStackEntry ->
                val animalId = backStackEntry.arguments?.getString("animalId") ?: ""
                if (token != null) {
                    AnimalDetailsScreen(animalId = animalId, token = token, navController = navController)
                }
            }

            composable("folderCamera/{token}/{folderId}?existingAnimalIds={existingAnimalIds}") { backStackEntry ->
                val token = backStackEntry.arguments?.getString("token") ?: ""
                val folderId = backStackEntry.arguments?.getString("folderId") ?: ""
                val existingAnimalIds = backStackEntry.arguments?.getString("existingAnimalIds")?.split(",") ?: emptyList()
                FolderCameraScreen(
                    token = token,
                    folderId = folderId,
                    navController = navController,
                    existingAnimalIds = existingAnimalIds
                )
            }

            composable("folderReadAnimals/{token}/{folderId}?scannedAnimalIds={scannedAnimalIds}") { backStackEntry ->
                val token = backStackEntry.arguments?.getString("token") ?: ""
                val folderId = backStackEntry.arguments?.getString("folderId") ?: ""
                val scannedAnimalIds = backStackEntry.arguments?.getString("scannedAnimalIds")?.split(",") ?: emptyList()
                FolderReadAnimalsScreen(
                    token = token,
                    folderId = folderId,
                    navController = navController,
                    scannedAnimalIds = scannedAnimalIds
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