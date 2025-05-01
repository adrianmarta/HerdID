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

@OptIn(ExperimentalMaterial3Api::class)
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
            text = "Login",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ID Input
        OutlinedTextField(
            value = id,
            onValueChange = { id = it },
            label = { Text("ID") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = MaterialTheme.colorScheme.background,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Gray
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // CNP Input
        OutlinedTextField(
            value = cnp,
            onValueChange = { cnp = it },
            label = { Text("CNP") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = MaterialTheme.colorScheme.background,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Gray
            )
        )

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

                        onLoginSuccess(response.token) // Pass token to home screen
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
                .padding(horizontal = 8.dp)
                .height(56.dp), // Larger button
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Login", color = Color.White, style = MaterialTheme.typography.bodyLarge)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Loading Indicator or Error Message
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewLoginScreen() {
    LoginScreen(onLoginSuccess = { token -> })
}
