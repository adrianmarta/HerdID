package com.example.farmerappfrontend
import com.example.farmerappfrontend.Folder

import androidx.compose.material.icons.filled.Folder
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(navController: NavController) {
    var folderName by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isFileDialogOpen by remember { mutableStateOf(false) }
    var files by remember { mutableStateOf<List<Folder>>(emptyList()) }
    var selectedFolders by remember { mutableStateOf<Set<String>>(emptySet()) } // Track selected folders

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

    // Function to delete selected folders
    fun deleteFolders(folderIds: Set<String>) {
        coroutineScope.launch {
            try {
                folderIds.forEach { folderId ->
                    val response = RetrofitClient.apiService.deleteFolder(folderId, "Bearer $token")
                    if (!response.isSuccessful) {
                        errorMessage = "Failed to delete folder: ${response.message()}"
                        return@forEach
                    }
                }
                // Refresh the folder list after deletion
                fetchFiles()
                selectedFolders = emptySet() // Clear selection
            } catch (e: Exception) {
                errorMessage = "Error deleting folders: ${e.message}"
            }
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
            title = { Text("Create Folder") },
            text = {
                Column {
                    Text("Enter folder name:")
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = fileName,
                        onValueChange = { fileName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Folder Name") }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HerdID") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(MaterialTheme.colorScheme.primary)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Display error message if any
            errorMessage?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Buttons (Add File and Delete)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { isFileDialogOpen = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Add Folder", color = MaterialTheme.colorScheme.onPrimary)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        deleteFolders(selectedFolders)
                    },
                    enabled = selectedFolders.isNotEmpty(), // Enable only if folders are selected
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.onPrimary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Existing Files Section
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), // Ensure it takes the available space
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(files) { folder ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = if (selectedFolders.contains(folder.id)) {
                            CardDefaults.cardColors(containerColor = Color(0xFF90CAF9)) // Highlight selected
                        } else {
                            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f).clickable {
                                    navController.navigate("folder/${folder.id}/animals") // Navigate to folder content
                                }
                            ) {
                                Text(
                                    folder.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Checkbox(
                                checked = selectedFolders.contains(folder.id),
                                onCheckedChange = { isChecked ->
                                    selectedFolders = if (isChecked) {
                                        selectedFolders + folder.id
                                    } else {
                                        selectedFolders - folder.id
                                    }
                                }
                            )
                        }
                    }
                }
            }
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