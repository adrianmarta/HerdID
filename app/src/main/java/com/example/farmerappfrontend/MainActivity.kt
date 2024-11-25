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

@Composable
fun MyApp() {
    val navController = rememberNavController()

    // Check if there is a saved token (this could be using SharedPreferences or another storage mechanism)
    val token = TokenManager.getToken(LocalContext.current)

    // Start navigation based on token existence
    NavHost(navController = navController, startDestination = if (token != null) "home/$token" else "login") {
        composable("login") {
            LoginScreen(
                onLoginSuccess = { token ->
                    // Save the token and navigate to home
                    TokenManager.saveToken(LocalContext.current, token)
                    navController.navigate("home/$token") {
                        popUpTo("login") { inclusive = true } // Pop the login screen from the back stack
                    }
                }
            )
        }
        composable("home/{token}") { backStackEntry ->
            val token = backStackEntry.arguments?.getString("token") ?: ""
            HomeScreen(
                token = token,
                onLogout = {
                    // This is where the logout logic happens
                    TokenManager.removeToken(LocalContext.current) // Remove the token (e.g., from SharedPreferences)
                    navController.popBackStack("login", false) // Navigate to the login screen
                }
            )
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
