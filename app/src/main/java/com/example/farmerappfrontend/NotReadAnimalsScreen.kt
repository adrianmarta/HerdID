
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotReadAnimalsScreen(
    token: String,
    animalIds: List<String>,
    ownerId: String,
    onNavigateBack: () -> Unit,
    folderId: String? = null
) {
    var currentAnimalIds by remember { mutableStateOf(animalIds) }
    var selectedAnimals by remember { mutableStateOf<Set<String>>(emptySet()) }
    var popupMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun reloadAnimalList() {
        scope.launch {
            try {
                if (folderId != null) {
                    val response = RetrofitClient.apiService.getAnimalsByFolderId(folderId, "Bearer $token")
                    if (response.isSuccessful) {
                        currentAnimalIds = response.body()?.map { it.id } ?: emptyList()
                    } else {
                        popupMessage = "Failed to reload animals: ${response.message()}"
                    }
                } else {
                    val response = RetrofitClient.apiService.getAnimalsByOwnerId(ownerId, "Bearer $token")
                    if (response.isSuccessful) {
                        currentAnimalIds = response.body()?.map { it.id } ?: emptyList()
                    } else {
                        popupMessage = "Failed to reload animals: ${response.message()}"
                    }
                }
            } catch (e: Exception) {
                popupMessage = "Error reloading animals: ${e.message}"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Not Read Animals") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(MaterialTheme.colorScheme.primary)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Animal List (Scrollable)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), // Ensures the LazyColumn takes available space
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(currentAnimalIds) { id ->
                    AnimalRowWithSelection(
                        animal = Animal(id, "unknown", ""),
                        isSelected = selectedAnimals.contains(id),
                        onToggleSelection = { animalId ->
                            selectedAnimals = if (selectedAnimals.contains(animalId)) {
                                selectedAnimals - animalId
                            } else {
                                selectedAnimals + animalId
                            }
                        }
                    )
                }
            }

            // Bottom Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectedAnimals.isNotEmpty() && folderId != null) {
                    Button(
                        onClick = {
                            scope.launch {
                                deleteSelectedAnimalsFromFolder(
                                    folderId,
                                    selectedAnimals.toList(),
                                    token
                                )
                                popupMessage = "Animals removed from folder successfully."
                                selectedAnimals = emptySet()
                                reloadAnimalList()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Remove from Folder", color = Color.White)
                    }
                }
                if (selectedAnimals.isNotEmpty())

                    Button(
                        onClick = {
                            scope.launch {
                                deleteAnimalsGlobally(selectedAnimals.toList(), token)
                                popupMessage = "Animals deleted globally successfully."
                                selectedAnimals = emptySet()
                                reloadAnimalList()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete Globally", color = Color.White)
                    }


                Button(
                    onClick = onNavigateBack,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Back", color = MaterialTheme.colorScheme.onSecondary)
                }
            }
        }
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

fun deleteSelectedAnimalsFromFolder(
    folderId: String,
    animalIds: List<String>,
    token: String
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = RetrofitClient.apiService.removeAnimalsFromFolder(folderId, animalIds, "Bearer $token")
            if (response.isSuccessful) {
                Log.d("DeleteFolder", "Animals removed from folder")
            } else {
                Log.e("DeleteFolder", "Failed to remove animals: ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("DeleteFolder", "Error removing animals from folder: ${e.message}")
        }
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
