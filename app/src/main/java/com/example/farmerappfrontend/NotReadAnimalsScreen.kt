package com.example.farmerappfrontend

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun NotReadAnimalsScreen(
    token: String,
    animalIds: List<String>,
    ownerId: String,
    onNavigateBack: () -> Unit,
    folderId: String? = null
) {
    var isFileDialogOpen by remember { mutableStateOf(false) }
    var folderName by remember { mutableStateOf("") }
    var popupMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    fun createFileWithAnimals(folderName: String) {
        coroutineScope.launch {
            try {
                // Step 1: Create folder
                val folderRequest = FolderRequest(name = folderName, ownerId = ownerId)
                val folderResponse = RetrofitClient.apiService.createFolder("Bearer $token", folderRequest)

                if (folderResponse.isSuccessful) {
                    val createdFolderId = folderResponse.body()?.id ?: return@launch

                    // Log folder creation success
                    Log.d("CreateFile", "Folder created: ID=$createdFolderId, Name=$folderName")

                    // Step 2: Add animals to the folder
                    if (animalIds.isNotEmpty()) {
                        val addAnimalsResponse = RetrofitClient.apiService.addAnimalsToFolder(
                            folderId = createdFolderId,
                            animalIds = animalIds,
                            authorization = "Bearer $token"
                        )

                        if (addAnimalsResponse.isSuccessful) {
                            Log.d("AddAnimals", "Animals added successfully to folder ID=$createdFolderId")
                            popupMessage = "File '$folderName' created and animals added successfully!"
                        } else {
                            Log.e("AddAnimals", "Failed to add animals: ${addAnimalsResponse.message()} (Code: ${addAnimalsResponse.code()})")
                            popupMessage = "Failed to add animals: ${addAnimalsResponse.message()}"
                        }
                    } else {
                        Log.e("AddAnimals", "No animals to add to folder.")
                        popupMessage = "File created but no animals were added (empty list)."
                    }
                } else {
                    Log.e("CreateFile", "Failed to create file: ${folderResponse.message()} (Code: ${folderResponse.code()})")
                    popupMessage = "Failed to create file: ${folderResponse.message()}"
                }
            } catch (e: Exception) {
                Log.e("CreateFile", "Error: ${e.message}")
                popupMessage = "Error creating file: ${e.message}"
            }
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Not Read Animals",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Animal List
        LazyColumn(
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(animalIds) { id ->
                Text(id, modifier = Modifier.padding(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Create File Button
        Button(
            onClick = { isFileDialogOpen = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create File with Not Read Animals")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Back Button
        Button(
            onClick = onNavigateBack, modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }

    // Dialog for Folder Name Input
    if (isFileDialogOpen) {
        AlertDialog(
            onDismissRequest = { isFileDialogOpen = false },
            title = { Text("Create File") },
            text = {
                Column {
                    Text("Enter file name:")
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = folderName,
                        onValueChange = { folderName = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (folderName.isNotBlank()) {
                            createFileWithAnimals(folderName)
                            isFileDialogOpen = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                Button(onClick = { isFileDialogOpen = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Popup Message
    popupMessage?.let {
        AlertDialog(
            onDismissRequest = { popupMessage = null },
            title = { Text("Action Result") },
            text = { Text(it) },
            confirmButton = {
                Button(onClick = { popupMessage = null }) {
                    Text("OK")
                }
            }
        )
    }
}

suspend fun deleteAnimalsGlobally(animalIds: List<String>, token: String) {
    for (id in animalIds) {
        try {
            val response = RetrofitClient.apiService.deleteAnimal(id, "Bearer $token")
            if (response.isSuccessful) {
                Log.d("Delete", "Animal $id deleted globally")
            } else {
                Log.e("Delete", "Failed to delete animal $id: ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("Delete", "Error deleting animal $id: ${e.message}")
        }
    }
}

fun deleteSelectedAnimalsFromFolder(
    folderId: String,
    animalIds: List<String>,
    token: String,
    onComplete: () -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = RetrofitClient.apiService.removeAnimalsFromFolder(folderId, animalIds, "Bearer $token")
            if (response.isSuccessful) {
                onComplete()
            } else {
                Log.e("DeleteFolder", "Failed to remove animals from folder: ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("DeleteFolder", "Error removing animals from folder: ${e.message}")
        }
    }
}

