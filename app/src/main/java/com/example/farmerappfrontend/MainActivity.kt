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
            HomeScreen(
                token = token,
                onLogout = {
                    TokenManager.removeToken(context)
                    navController.popBackStack("login", false)
                },
                navController = navController
            )
        }
        composable("camera") { CameraScreen() }
        composable("animals") { AnimalListScreen(token = token ?: "") }
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
