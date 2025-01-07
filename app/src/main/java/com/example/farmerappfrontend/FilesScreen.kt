package com.example.farmerappfrontend

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@Composable
fun FilesScreen(navController: NavController) {
    var folderName by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isFileDialogOpen by remember { mutableStateOf(false) }
    var files by remember { mutableStateOf<List<Folder>>(emptyList()) } // Now stores a list of Folder objects

    val token = TokenManager.getToken(navController.context)
    val coroutineScope = rememberCoroutineScope()

    // Fetch files from backend
    fun fetchFiles() {
        coroutineScope.launch {
            try {
                val userProfile = RetrofitClient.apiService.getUserProfile("Bearer $token")
                val ownerId = userProfile.id
                val response = RetrofitClient.apiService.getFolders("Bearer $token", ownerId)
                if (response.isSuccessful) {
                    files = response.body() ?: emptyList()
                } else {
                    errorMessage = "Failed to fetch files: ${response.message()}"
                }
            } catch (e: Exception) {
                errorMessage = "Error fetching files: ${e.message}"
            }
        }
    }

    // Function to create a folder in the backend
    fun createFolder(folderName: String) {
        if (folderName.isNotBlank()) {
            isCreating = true
            coroutineScope.launch {
                try {
                    val userProfile = RetrofitClient.apiService.getUserProfile("Bearer $token")
                    val ownerId = userProfile.id
                    val folderRequest = FolderRequest(name = folderName, ownerId = ownerId)

                    val response = RetrofitClient.apiService.createFolder("Bearer $token", folderRequest)
                    if (response.isSuccessful) {
                        Toast.makeText(navController.context, "Folder '$folderName' created successfully.", Toast.LENGTH_SHORT).show()
                        fetchFiles() // Refresh the list after creating a folder
                    } else {
                        errorMessage = "Failed to create folder: ${response.message()}"
                    }
                } catch (e: Exception) {
                    errorMessage = "Error creating folder: ${e.message}"
                } finally {
                    isCreating = false
                }
            }
        } else {
            errorMessage = "Folder name cannot be empty."
        }
    }

    // Fetch files when the screen is first loaded
    LaunchedEffect(Unit) {
        fetchFiles()
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
                            .background(Color.Gray.copy(alpha = 0.1f))
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

        // Display error message if any
        if (errorMessage != null) {
            Text(
                errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Existing Files Section
        Text("Existing Files:", fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(8.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            files.forEach { folder ->
                Text(
                    folder.name, // Display the folder name
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .clickable {
                            navController.navigate("folder/${folder.id}/animals") // Navigate to AnimalListScreenByFolder
                        },
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyLarge
                )
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
                createFolder(newFile)
            }
        )
    }
}
