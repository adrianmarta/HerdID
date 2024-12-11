package com.example.farmerappfrontend

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@Composable
fun FilesScreen(navController: NavController) {
    var folderName by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isFileDialogOpen by remember { mutableStateOf(false) }
    var files by remember { mutableStateOf(listOf("File1.txt", "File2.docx")) } // Mock file data

    val token = TokenManager.getToken(navController.context)
    val coroutineScope = rememberCoroutineScope()

    fun createFolder() {
        if (folderName.isNotBlank()) {
            isCreating = true
            errorMessage = null
            coroutineScope.launch {
                try {
                    val userProfile = RetrofitClient.apiService.getUserProfile("Bearer $token")
                    val ownerId = userProfile.id

                    val folderRequest = FolderRequest(name = folderName, ownerId = ownerId)
                    val response = RetrofitClient.apiService.createFolder("Bearer $token", folderRequest)

                    if (response.isSuccessful) {
                        Toast.makeText(navController.context, "Folder '$folderName' created successfully.", Toast.LENGTH_SHORT).show()
                        folderName = ""
                    } else {
                        errorMessage = "Failed to create folder: ${response.message()}"
                    }

                    isCreating = false
                } catch (e: Exception) {
                    errorMessage = "Error: ${e.message}"
                    isCreating = false
                }
            }
        } else {
            errorMessage = "Please enter a folder name."
        }
    }

    @Composable
    fun FileCreationDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
        var fileName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Create File") },
            text = {
                Column {
                    Text("Enter file name:")
                    Spacer(modifier = Modifier.height(8.dp))
                    BasicTextField(
                        value = fileName,
                        onValueChange = { fileName = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Gray.copy(alpha = 0.1f), shape = MaterialTheme.shapes.small)
                            .padding(16.dp)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onCreate(fileName)
                    onDismiss()
                }) {
                    Text("Create")
                }
            },
            dismissButton = {
                Button(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text("Files Screen", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        // Folder Creation Section
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BasicTextField(
                value = folderName,
                onValueChange = { folderName = it },
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { createFolder() }
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .background(Color.Gray.copy(alpha = 0.1f), shape = MaterialTheme.shapes.small)
                    .padding(16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (errorMessage != null) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { createFolder() },
                enabled = !isCreating,
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                if (isCreating) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Create Folder", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Existing Files Section
        Text("Existing Files:", fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(8.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            files.forEach { file ->
                Text(file, modifier = Modifier.padding(vertical = 4.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Create File Button
        Button(
            onClick = { isFileDialogOpen = true },
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Text("Create File")
        }
    }

    if (isFileDialogOpen) {
        FileCreationDialog(
            onDismiss = { isFileDialogOpen = false },
            onCreate = { newFile ->
                if (newFile.isNotBlank()) {
                    files = files + newFile
                    Toast.makeText(navController.context, "File '$newFile' created successfully.", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}
