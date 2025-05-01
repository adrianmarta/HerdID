package com.example.farmerappfrontend
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.border
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import kotlinx.coroutines.launch
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    navToLogin: () -> Unit
) {
    var id by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Register",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ID
        OutlinedTextField(
            value = id,
            onValueChange = { id = it },
            label = { Text("ID (e.g. RO1234567890)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Email
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Name
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // DOB
        OutlinedTextField(
            value = dob,
            onValueChange = { dob = it },
            label = { Text("Date of Birth (YYYY-MM-DD)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Address
        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Address") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Phone Number
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("Phone Number") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Submit
        Button(
            onClick = {
                isLoading = true
                message = ""
                scope.launch {
                    try {
                        val request = RegisterRequest(id, email, password, name, dob, address, phoneNumber)
                        val response = RetrofitClient.apiService.register(request)
                        if (response.isSuccessful) {
                            message = "Account created successfully!"
                            onRegisterSuccess()
                        } else {
                            message = "Register failed: ${response.message()}"
                        }
                    } catch (e: Exception) {
                        message = "Error: ${e.message}"
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Register", color = Color.White)
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = navToLogin) {
            Text("Already have an account? Login")
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(8.dp))
        }

        if (message.isNotEmpty()) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}
