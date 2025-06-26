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

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.farmerappfrontend.ui.theme.FarmerAppFrontendTheme
import androidx.lifecycle.viewmodel.compose.viewModel


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

    var startDestination by remember { mutableStateOf<String?>(null) }
    val cameraViewModel: CameraViewModel = viewModel()
    val folderCameraViewModel: FolderCameraViewModel = viewModel()

    LaunchedEffect(Unit) {
        val token = TokenManager.getToken(context)
        if (token != null) {
            try {
                RetrofitClient.apiService.getUserProfile("Bearer $token")
                startDestination = "home/$token"
            } catch (e: Exception) {
                startDestination = "login"
            }
        } else {
            startDestination = "login"
        }
    }

    if (startDestination == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        NavHost(
            navController = navController,
            startDestination = startDestination!!
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
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    navController = navController
                )
            }
            composable("animals/{token}") { backStackEntry ->
                val token = backStackEntry.arguments?.getString("token") ?: ""
                AnimalListScreen(token = token, navController = navController, cameraViewModel = cameraViewModel)
            }

            composable("camera/{token}/{existingAnimalIds}") { backStackEntry ->
                val token = backStackEntry.arguments?.getString("token") ?: ""
                val existingAnimalIds = backStackEntry.arguments
                    ?.getString("existingAnimalIds")
                    ?.split(",") ?: emptyList()

                CameraScreen(
                    token = token,
                    navController = navController,
                    existingAnimalIds = existingAnimalIds,
                    cameraViewModel = cameraViewModel
                )
            }
            composable("fileUpload/{token}")
            {
                backStackEntry ->
                val token = backStackEntry.arguments?.getString("token") ?: ""
                AddAnimalsScreen(token=token,navController=navController)
            }
            composable("files/{token}") { backStackEntry ->
                val token = backStackEntry.arguments?.getString("token") ?: ""
                FilesScreen(navController, token)
            }

            composable("folder/{folderId}/animals/{token}") { backStackEntry ->
                val folderId = backStackEntry.arguments?.getString("folderId") ?: ""
                val token = backStackEntry.arguments?.getString("token") ?: ""
                AnimalListScreenByFolder(
                    token = token,
                    folderId = folderId,
                    navController = navController,
                    folderCameraViewModel = folderCameraViewModel
                )
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

            composable("readAnimals/{token}") { backStackEntry ->
                val token = backStackEntry.arguments?.getString("token") ?: ""
                ReadAnimalsScreen(
                    token = token,
                    navController = navController,
                    cameraViewModel = cameraViewModel
                )
            }

            composable("statistics/{token}") { backStackEntry ->
                val token = backStackEntry.arguments?.getString("token") ?: ""
                StatisticsScreen(
                    navController = navController,
                    token = token
                )
            }

            composable("sessions/{token}") { backStackEntry ->
                val token = backStackEntry.arguments?.getString("token") ?: ""
                SessionsScreen(
                    navController = navController,
                    cameraViewModel = cameraViewModel,
                    folderCameraViewModel = folderCameraViewModel

                )
            }

            composable("transfers/{token}") { backStackEntry ->
                val token = backStackEntry.arguments?.getString("token") ?: ""
                TransfersScreen(navController = navController, token = token)
            }
            composable("createTransfer/{token}") { backStackEntry ->
                val token = backStackEntry.arguments?.getString("token") ?: ""
                CreateTransferScreen(navController = navController, token = token)
            }
            composable("addSingleAnimal/{token}/{animalId}") { backStackEntry ->
                val token = backStackEntry.arguments?.getString("token") ?: ""
                val animalId = backStackEntry.arguments?.getString("animalId") ?: ""
                AddSingleAnimalScreen(token = token, animalId = animalId, navController = navController)
            }

            composable("animalDetails/{animalId}/{token}") { backStackEntry ->
                val animalId = backStackEntry.arguments?.getString("animalId") ?: ""
                val token = backStackEntry.arguments?.getString("token") ?: ""
                AnimalDetailsScreen(animalId = animalId, token = token, navController = navController)
            }

            composable("folderCamera/{token}/{folderId}?existingAnimalIds={existingAnimalIds}") { backStackEntry ->
                val token = backStackEntry.arguments?.getString("token") ?: ""
                val folderId = backStackEntry.arguments?.getString("folderId") ?: ""
                val existingAnimalIds = backStackEntry.arguments?.getString("existingAnimalIds")?.split(",") ?: emptyList()
                FolderCameraScreen(
                    token = token,
                    folderId = folderId,
                    navController = navController,
                    existingAnimalIds = existingAnimalIds,
                    folderCameraViewModel = folderCameraViewModel
                )
            }

            composable("folderReadAnimals/{token}/{folderId}") { backStackEntry ->
                val token = backStackEntry.arguments?.getString("token") ?: ""
                val folderId = backStackEntry.arguments?.getString("folderId") ?: ""
                FolderReadAnimalsScreen(
                    token = token,
                    folderId = folderId,
                    navController = navController,
                    folderCameraViewModel = folderCameraViewModel
                )
            }
            composable("createTransfer/{token}") { backStackEntry ->
                val token = backStackEntry.arguments?.getString("token") ?: ""
                CreateTransferScreen(navController = navController, token = token)
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