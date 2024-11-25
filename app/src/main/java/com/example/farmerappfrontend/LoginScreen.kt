// LoginScreen.kt
package com.example.farmerappfrontend

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        Text("Login", style = MaterialTheme.typography.headlineLarge)

        Spacer(modifier = Modifier.height(16.dp))

        BasicTextField(
            value = id,
            onValueChange = { id = it },
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            textStyle = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        BasicTextField(
            value = cnp,
            onValueChange = { cnp = it },
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            textStyle = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

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
            enabled = !isLoading
        ) {
            Text("Login")
        }

        if (isLoading) {
            CircularProgressIndicator()
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

