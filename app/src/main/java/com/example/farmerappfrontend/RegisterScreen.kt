package com.example.farmerappfrontend

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import retrofit2.HttpException

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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    @Composable
    fun textFieldColors() = TextFieldDefaults.outlinedTextFieldColors(
        containerColor = MaterialTheme.colorScheme.surface,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = Color.Gray
    )

    fun validateInputs(): Boolean {
        return when {
            id.isBlank() -> {
                Toast.makeText(context, "ID is required", Toast.LENGTH_SHORT).show()
                false
            }
            email.isBlank() -> {
                Toast.makeText(context, "Email is required", Toast.LENGTH_SHORT).show()
                false
            }
            password.isBlank() -> {
                Toast.makeText(context, "Password is required", Toast.LENGTH_SHORT).show()
                false
            }
            name.isBlank() -> {
                Toast.makeText(context, "Name is required", Toast.LENGTH_SHORT).show()
                false
            }
            dob.isBlank() -> {
                Toast.makeText(context, "Date of Birth is required", Toast.LENGTH_SHORT).show()
                false
            }
            address.isBlank() -> {
                Toast.makeText(context, "Address is required", Toast.LENGTH_SHORT).show()
                false
            }
            phoneNumber.isBlank() -> {
                Toast.makeText(context, "Phone Number is required", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Register", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = id,
            onValueChange = { id = it },
            label = { Text("ID (e.g. RO1234567890)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = textFieldColors()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = textFieldColors()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = textFieldColors()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = textFieldColors()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = dob,
            onValueChange = { dob = it },
            label = { Text("Date of Birth (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = textFieldColors()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Address") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = textFieldColors()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("Phone Number") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = textFieldColors()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (!validateInputs()) return@Button
                isLoading = true
                scope.launch {
                    try {
                        val request = RegisterRequest(id, email, password, name, dob, address, phoneNumber)
                        val response = RetrofitClient.apiService.register(request)
                        if (response.isSuccessful) {
                            Toast.makeText(context, "Account created successfully!", Toast.LENGTH_LONG).show()
                            onRegisterSuccess()
                        } else {
                            val message = when (response.code()) {
                                400 -> "ID already exists. Please use a different ID."
                                else -> "Register failed: ${response.message()}"
                            }
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }
                    } catch (e: HttpException) {
                        Toast.makeText(context, "HTTP Error: ${e.message()}", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Unexpected Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
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
    }
}
