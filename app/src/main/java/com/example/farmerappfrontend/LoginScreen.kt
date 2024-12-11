package com.example.farmerappfrontend

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.border
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoginSuccess: (String) -> Unit) {
    var id by remember { mutableStateOf("") }
    var cnp by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Title
        Text(
            "Login",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ID Input
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("ID", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            BasicTextField(
                value = id,
                onValueChange = { id = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp) // Make input box taller
                    .padding(16.dp)
                    .border(1.dp, Color.Gray), // Border around the input box
                textStyle = MaterialTheme.typography.bodyLarge // Larger text to match the size of the input box
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // CNP Input
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("CNP", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            BasicTextField(
                value = cnp,
                onValueChange = { cnp = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp) // Make input box taller
                    .padding(16.dp)
                    .border(1.dp, Color.Gray), // Border around the input box
                textStyle = MaterialTheme.typography.bodyLarge // Larger text to match the size of the input box
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Login Button
        Button(
            onClick = {
                isLoading = true
                errorMessage = ""
                scope.launch {
                    try {
                        val loginRequest = LoginRequest(id, cnp)
                        val response = RetrofitClient.apiService.login(loginRequest)

                        onLoginSuccess(response.token)  // Passing token to home screen
                    } catch (e: Exception) {
                        errorMessage = "Login failed: ${e.message}"
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp), // Making button bigger
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8A56E2))  // The original purple color
        ) {
            Text("Login", color = Color.White)  // White text color for better contrast
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Loading or error message
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        }

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewLoginScreen() {
    LoginScreen(onLoginSuccess = { token -> })
}
