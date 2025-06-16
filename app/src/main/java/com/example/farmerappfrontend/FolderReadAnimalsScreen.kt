package com.example.farmerappfrontend

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import android.util.Log
import androidx.navigation.NavController
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderReadAnimalsScreen(
    token: String,
    folderId: String,
    navController: NavController,
    scannedAnimalIds: List<String>
) {
    var animalsWithStatus by remember { mutableStateOf<List<ScannedAnimalStatus>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var popupMessage by remember { mutableStateOf<String?>(null) } // For displaying messages like adding to folder result
    val scope = rememberCoroutineScope()
    var selectedAnimalIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isShowingNotRead by remember { mutableStateOf(false) }
    var notReadAnimalIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var allAnimals by remember { mutableStateOf<List<Animal>>(emptyList()) }

    // State for category selection checkboxes
    var selectAllInFolder by remember { mutableStateOf(false) }
    var selectAllNotInFolderExistsGlobally by remember { mutableStateOf(false) }
    var selectAllNewAnimal by remember { mutableStateOf(false) }

    var showEventDialog by remember { mutableStateOf(false) } // State to control EventDialog visibility

    // Fetch all animals and calculate not read animals
    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.apiService.getAnimalsByFolderId(folderId, "Bearer $token")
            if (response.isSuccessful) {
                allAnimals = response.body() ?: emptyList()
                // Calculate not read animals (animals in folder that weren't scanned)
                notReadAnimalIds = allAnimals.map { it.id }.filterNot { scannedAnimalIds.contains(it) }
            }
        } catch (e: Exception) {
            popupMessage = "Error fetching folder animals: ${e.message}"
        }
    }

    // Update animal statuses based on current view
    LaunchedEffect(scannedAnimalIds, isShowingNotRead, allAnimals) {
        isLoading = true
        val results = mutableListOf<ScannedAnimalStatus>()
        
        if (isShowingNotRead) {
            // Show not read animals
            for (id in notReadAnimalIds) {
                try {
                    val animalDetailsResponse = RetrofitClient.apiService.getAnimal(id, "Bearer $token")
                    if (animalDetailsResponse.isSuccessful) {
                        results.add(ScannedAnimalStatus(animalDetailsResponse.body(), id, AnimalStatus.IN_FOLDER))
                    } else {
                        results.add(ScannedAnimalStatus(id = id, status = AnimalStatus.IN_FOLDER))
                    }
                } catch (e: Exception) {
                    Log.e("FolderReadAnimals", "Error processing not read ID $id: ${e.message}")
                }
            }
        } else {
            // Show scanned animals with their status
            for (id in scannedAnimalIds) {
                try {
                    val existsGloballyResponse = RetrofitClient.apiService.checkAnimalExists(id, "Bearer $token")
                    if (existsGloballyResponse.isSuccessful && existsGloballyResponse.body() == true) {
                        val folderAnimalsResponse = RetrofitClient.apiService.getAnimalsByFolderId(folderId, "Bearer $token")
                        val isInFolder = folderAnimalsResponse.isSuccessful && (folderAnimalsResponse.body()?.any { it.id == id } == true)

                        if (isInFolder) {
                            val animalDetailsResponse = RetrofitClient.apiService.getAnimal(id, "Bearer $token")
                            if (animalDetailsResponse.isSuccessful) {
                                results.add(ScannedAnimalStatus(animalDetailsResponse.body(), id, AnimalStatus.IN_FOLDER))
                            } else {
                                results.add(ScannedAnimalStatus(id = id, status = AnimalStatus.IN_FOLDER))
                            }
                        } else {
                            val animalDetailsResponse = RetrofitClient.apiService.getAnimal(id, "Bearer $token")
                            if (animalDetailsResponse.isSuccessful) {
                                results.add(ScannedAnimalStatus(animalDetailsResponse.body(), id, AnimalStatus.NOT_IN_FOLDER_EXISTS_GLOBALLY))
                            } else {
                                results.add(ScannedAnimalStatus(id = id, status = AnimalStatus.NOT_IN_FOLDER_EXISTS_GLOBALLY))
                            }
                        }
                    } else {
                        results.add(ScannedAnimalStatus(id = id, status = AnimalStatus.NEW_ANIMAL))
                    }
                } catch (e: Exception) {
                    Log.e("FolderReadAnimals", "Error processing scanned ID $id: ${e.message}")
                    results.add(ScannedAnimalStatus(id = id, status = AnimalStatus.NEW_ANIMAL))
                }
            }
        }
        animalsWithStatus = results.sortedBy { it.id }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (isShowingNotRead) "Not Read Animals (${notReadAnimalIds.size})" 
                        else "Scanned Animals (${scannedAnimalIds.size})",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { 
                            isShowingNotRead = !isShowingNotRead
                            selectedAnimalIds = emptySet()
                        }
                    ) {
                        Icon(
                            if (isShowingNotRead) Icons.Default.CheckCircle else Icons.Default.List,
                            contentDescription = if (isShowingNotRead) "Show Scanned Animals" else "Show Not Read Animals"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(MaterialTheme.colorScheme.primary)
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                // Top Action Buttons (Add to Folder, Add Event)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Add to Folder Button
                    Button(
                        onClick = {
                            /* TODO: Implement Add to Folder */ Log.d("FolderReadAnimals", "Add selected to folder: ${selectedAnimalIds.joinToString()}")
                            val animalsToAdd = animalsWithStatus.filter { it.id in selectedAnimalIds && it.status == AnimalStatus.NOT_IN_FOLDER_EXISTS_GLOBALLY }.map { it.id }
                            if (animalsToAdd.isNotEmpty()) {
                                scope.launch {
                                    try {
                                        val response = RetrofitClient.apiService.addAnimalsToFolder(
                                            folderId = folderId,
                                            animalIds = animalsToAdd,
                                            authorization = "Bearer $token"
                                        )
                                        if (response.isSuccessful) {
                                            popupMessage = "Successfully added ${animalsToAdd.size} animals to folder."
                                            selectedAnimalIds = emptySet() // Clear selection after adding
                                            // Refresh the list to update statuses
                                            // This will re-trigger the LaunchedEffect to fetch and update statuses
                                            // Alternatively, manually update the status of added animals in animalsWithStatus list
                                            // For simplicity now, let's re-fetch:
                                            // Re-fetch animals in folder to update their status to IN_FOLDER
                                            isLoading = true // Show loading while refetching
                                            val updatedAnimals = mutableListOf<ScannedAnimalStatus>()
                                            for (id in scannedAnimalIds) { // Process all scanned IDs again
                                                try {
                                                    val existsGloballyResponse = RetrofitClient.apiService.checkAnimalExists(id, "Bearer $token")
                                                    if (existsGloballyResponse.isSuccessful && existsGloballyResponse.body() == true) {
                                                        val folderAnimalsResponse = RetrofitClient.apiService.getAnimalsByFolderId(folderId, "Bearer $token")
                                                        val isInFolder = folderAnimalsResponse.isSuccessful && (folderAnimalsResponse.body()?.any { it.id == id } == true)

                                                        if (isInFolder) {
                                                            val animalDetailsResponse = RetrofitClient.apiService.getAnimal(id, "Bearer $token")
                                                            if (animalDetailsResponse.isSuccessful) {
                                                                updatedAnimals.add(ScannedAnimalStatus(animalDetailsResponse.body(), id, AnimalStatus.IN_FOLDER))
                                                            } else {
                                                                updatedAnimals.add(ScannedAnimalStatus(id = id, status = AnimalStatus.IN_FOLDER))
                                                                Log.e("FolderReadAnimals", "Failed to fetch details for $id: ${animalDetailsResponse.message()}")
                                                            }
                                                        } else {
                                                            val animalDetailsResponse = RetrofitClient.apiService.getAnimal(id, "Bearer $token")
                                                            if (animalDetailsResponse.isSuccessful) {
                                                                updatedAnimals.add(ScannedAnimalStatus(animalDetailsResponse.body(), id, AnimalStatus.NOT_IN_FOLDER_EXISTS_GLOBALLY))
                                                            } else {
                                                                updatedAnimals.add(ScannedAnimalStatus(id = id, status = AnimalStatus.NOT_IN_FOLDER_EXISTS_GLOBALLY))
                                                                Log.e("FolderReadAnimals", "Failed to fetch details for $id: ${animalDetailsResponse.message()}")
                                                            }
                                                        }
                                                    } else {
                                                        updatedAnimals.add(ScannedAnimalStatus(id = id, status = AnimalStatus.NEW_ANIMAL))
                                                    }
                                                } catch (e: Exception) {
                                                    Log.e("FolderReadAnimals", "Error processing scanned ID $id during refresh: ${e.message}")
                                                    updatedAnimals.add(ScannedAnimalStatus(id = id, status = AnimalStatus.NEW_ANIMAL))
                                                }
                                            }
                                            animalsWithStatus = updatedAnimals.sortedBy { it.id }
                                            isLoading = false

                                        } else {
                                            popupMessage = "Failed to add animals to folder: ${response.message()}"
                                        }
                                    } catch (e: Exception) {
                                        popupMessage = "Error adding animals to folder: ${e.message}"
                                    }
                                }
                            } else {
                                popupMessage = "Select animals with 'Not Present' status to add."
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = selectedAnimalIds.isNotEmpty() && animalsWithStatus.filter { it.id in selectedAnimalIds }.all { it.status == AnimalStatus.NOT_IN_FOLDER_EXISTS_GLOBALLY } // Enabled only for NOT_IN_FOLDER_EXISTS_GLOBALLY selected
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Add to Folder")
                    }
                    // Add Event Button
                    Button(
                        onClick = {
                            val eligibleAnimalIds = animalsWithStatus
                                .filter { it.id in selectedAnimalIds && (it.status == AnimalStatus.IN_FOLDER || it.status == AnimalStatus.NOT_IN_FOLDER_EXISTS_GLOBALLY) }
                                .map { it.id }
                            if (eligibleAnimalIds.isNotEmpty()) {
                                // Pass the eligible selected animal IDs to the dialog
                                showEventDialog = true
                            } else {
                                popupMessage = "Select animals that exist globally to add an event."
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = selectedAnimalIds.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Event, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Add Event")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Selection Checkboxes (by Status Category)
                Column {
                    // Select Present (In Folder) Checkbox
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = selectAllInFolder,
                            onCheckedChange = { checked ->
                                selectAllInFolder = checked
                                val inFolderIds = animalsWithStatus.filter { it.status == AnimalStatus.IN_FOLDER }.map { it.id }.toSet()
                                selectedAnimalIds = if (checked) selectedAnimalIds + inFolderIds else selectedAnimalIds - inFolderIds
                            }
                        )
                        Text("Select Present (${animalsWithStatus.count { it.status == AnimalStatus.IN_FOLDER }})",
                            modifier = Modifier.clickable { selectAllInFolder = !selectAllInFolder }
                        )
                    }
                    // Select Not Present (Exists Globally) Checkbox
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = selectAllNotInFolderExistsGlobally,
                            onCheckedChange = { checked ->
                                selectAllNotInFolderExistsGlobally = checked
                                val notInFolderIds = animalsWithStatus.filter { it.status == AnimalStatus.NOT_IN_FOLDER_EXISTS_GLOBALLY }.map { it.id }.toSet()
                                selectedAnimalIds = if (checked) selectedAnimalIds + notInFolderIds else selectedAnimalIds - notInFolderIds
                            }
                        )
                        Text("Select Not Present (${animalsWithStatus.count { it.status == AnimalStatus.NOT_IN_FOLDER_EXISTS_GLOBALLY }})",
                           modifier = Modifier.clickable { selectAllNotInFolderExistsGlobally = !selectAllNotInFolderExistsGlobally }
                        )
                    }
                    // Select New Animals Checkbox
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = selectAllNewAnimal,
                            onCheckedChange = { checked ->
                                selectAllNewAnimal = checked
                                val newAnimalIds = animalsWithStatus.filter { it.status == AnimalStatus.NEW_ANIMAL }.map { it.id }.toSet()
                                selectedAnimalIds = if (checked) selectedAnimalIds + newAnimalIds else selectedAnimalIds - newAnimalIds
                            }
                        )
                        Text("Select New Animals (${animalsWithStatus.count { it.status == AnimalStatus.NEW_ANIMAL }})",
                           modifier = Modifier.clickable { selectAllNewAnimal = !selectAllNewAnimal }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp)) // Space between checkboxes and list

                // Animal List
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(animalsWithStatus) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedAnimalIds = if (selectedAnimalIds.contains(item.id)) {
                                        selectedAnimalIds - item.id
                                    } else {
                                        selectedAnimalIds + item.id
                                    }
                                },
                            elevation = CardDefaults.elevatedCardElevation(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedAnimalIds.contains(item.id)) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            )
                        ) {
                            Row( // Use Row to place content and checkmark/button side by side
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween // Space between content and checkmark/button
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) { // Allow column to take available space, add end padding
                                    // Animal ID and Gender (if available)
                                    val genderSymbol = if (item.animal?.gender?.equals("male", ignoreCase = true) == true) "♂" else if (item.animal?.gender?.equals("female", ignoreCase = true) == true) "♀" else ""
                                    Text("${item.id} $genderSymbol", style = MaterialTheme.typography.bodyLarge)

                                    // Birth Date (if available)
                                    item.animal?.birthDate?.let { birthDate ->
                                        Text(
                                            text = "Born: ${birthDate}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    // Status Text
                                    Text("Status: ${when(item.status) { AnimalStatus.IN_FOLDER -> "Present"; AnimalStatus.NOT_IN_FOLDER_EXISTS_GLOBALLY -> "Not Present"; AnimalStatus.NEW_ANIMAL -> "New" }}", style = MaterialTheme.typography.bodyMedium) // Display user-friendly status

                                    // Show Add button for New Animals - placed below details
                                    if (item.status == AnimalStatus.NEW_ANIMAL) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(onClick = { navController.navigate("addSingleAnimal/$token/${item.id}") }) {
                                            Text("Add Animal Globally")
                                        }
                                    }
                                }

                                // Right side of the card: Checkmark, Add button, and Details icon
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Checkmark Icon (Visible when selected and not a New Animal)
                                    if (selectedAnimalIds.contains(item.id) && item.status != AnimalStatus.NEW_ANIMAL) { 
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp)) // Space between checkmark and details icon
                                    }

                                    // Details Icon
                                    IconButton(onClick = { navController.navigate("animalDetails/${item.id}") }) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "Details"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Action Buttons (Delete and Show Not Read)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Delete Selected Button
                    Button(
                        onClick = {
                            val animalIdsToDelete = selectedAnimalIds.toList()
                            if (animalIdsToDelete.isNotEmpty()) {
                                scope.launch {
                                    try {
                                        val response = RetrofitClient.apiService.removeAnimalsFromFolder(
                                            folderId = folderId,
                                            animalIds = animalIdsToDelete,
                                            token = "Bearer $token"
                                        )
                                        if (response.isSuccessful) {
                                            popupMessage = "Successfully removed ${animalIdsToDelete.size} animals from folder."
                                            selectedAnimalIds = emptySet()
                                            
                                            // Update local state after deletion
                                            if (isShowingNotRead) {
                                                // Remove deleted animals from allAnimals and recalculate notReadAnimalIds
                                                allAnimals = allAnimals.filter { it.id !in selectedAnimalIds }
                                                notReadAnimalIds = allAnimals.map { it.id }.filterNot { scannedAnimalIds.contains(it) }
                                            }
                                            
                                            // Refresh the list
                                            isLoading = true
                                            val updatedAnimals = mutableListOf<ScannedAnimalStatus>()
                                            val idsToProcess = if (isShowingNotRead) notReadAnimalIds else scannedAnimalIds
                                            
                                            for (id in idsToProcess) {
                                                try {
                                                    if (isShowingNotRead) {
                                                        val animalDetailsResponse = RetrofitClient.apiService.getAnimal(id, "Bearer $token")
                                                        if (animalDetailsResponse.isSuccessful) {
                                                            updatedAnimals.add(ScannedAnimalStatus(animalDetailsResponse.body(), id, AnimalStatus.IN_FOLDER))
                                                        }
                                                    } else {
                                                        val existsGloballyResponse = RetrofitClient.apiService.checkAnimalExists(id, "Bearer $token")
                                                        if (existsGloballyResponse.isSuccessful && existsGloballyResponse.body() == true) {
                                                            val folderAnimalsResponse = RetrofitClient.apiService.getAnimalsByFolderId(folderId, "Bearer $token")
                                                            val isInFolder = folderAnimalsResponse.isSuccessful && (folderAnimalsResponse.body()?.any { it.id == id } == true)
                                                            
                                                            if (isInFolder) {
                                                                val animalDetailsResponse = RetrofitClient.apiService.getAnimal(id, "Bearer $token")
                                                                if (animalDetailsResponse.isSuccessful) {
                                                                    updatedAnimals.add(ScannedAnimalStatus(animalDetailsResponse.body(), id, AnimalStatus.IN_FOLDER))
                                                                }
                                                            } else {
                                                                val animalDetailsResponse = RetrofitClient.apiService.getAnimal(id, "Bearer $token")
                                                                if (animalDetailsResponse.isSuccessful) {
                                                                    updatedAnimals.add(ScannedAnimalStatus(animalDetailsResponse.body(), id, AnimalStatus.NOT_IN_FOLDER_EXISTS_GLOBALLY))
                                                                }
                                                            }
                                                        } else {
                                                            updatedAnimals.add(ScannedAnimalStatus(id = id, status = AnimalStatus.NEW_ANIMAL))
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    Log.e("FolderReadAnimals", "Error processing ID $id during refresh: ${e.message}")
                                                }
                                            }
                                            animalsWithStatus = updatedAnimals.sortedBy { it.id }
                                            isLoading = false
                                        } else {
                                            popupMessage = "Failed to remove animals from folder: ${response.message()}"
                                        }
                                    } catch (e: Exception) {
                                        popupMessage = "Error removing animals from folder: ${e.message}"
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        enabled = selectedAnimalIds.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Delete Selected")
                    }
                }
            }
        }
    }

    // Popup Message
    popupMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { popupMessage = null },
            title = { Text("Action Result") },
            text = { Text(message) },
            confirmButton = {
                Button(onClick = { popupMessage = null }) {
                    Text("OK")
                }
            }
        )
    }

    // Event Dialog
    if (showEventDialog) {
        // Pass the list of selected animal IDs to the dialog's onSave lambda
        EventDialog(
            onDismiss = { showEventDialog = false },
            onSave = { eventType, details ->
                showEventDialog = false

                // Implement batch posting of event for selected animals
                val eligibleAnimalIds = animalsWithStatus // Re-filter to get eligible IDs again if needed, or pass them to the dialog
                    .filter { it.id in selectedAnimalIds && (it.status == AnimalStatus.IN_FOLDER || it.status == AnimalStatus.NOT_IN_FOLDER_EXISTS_GLOBALLY) }
                    .map { it.id }

                scope.launch {
                    var successfulPosts = 0
                    var failedPosts = 0
                    for (animalId in eligibleAnimalIds) {
                        try {
                            val response = RetrofitClient.apiService.postEvent(animalId, "Bearer $token", details)
                            if (response.isSuccessful) {
                                Log.d("FolderReadAnimalsEvent", "Event posted for $animalId")
                                successfulPosts++
                            } else {
                                Log.e("FolderReadAnimalsEvent", "Failed to post event for $animalId: ${response.message()}")
                                failedPosts++
                            }
                        } catch (e: Exception) {
                            Log.e("FolderReadAnimalsEvent", "Error posting event for $animalId: ${e.message}")
                            failedPosts++
                        }
                    }
                    // Show summary popup message
                    popupMessage = "Event posting complete: $successfulPosts successful, $failedPosts failed."
                    selectedAnimalIds = emptySet() // Clear selection after attempting to post events
                }
            }
        )
    }
}
suspend fun reloadAnimalsWithStatus(
    folderId: String,
    scannedAnimalIds: List<String>,
    token: String
): List<ScannedAnimalStatus> {
    val updatedAnimals = mutableListOf<ScannedAnimalStatus>()
    for (id in scannedAnimalIds) {
        try {
            val existsGloballyResponse = RetrofitClient.apiService.checkAnimalExists(id, "Bearer $token")
            if (existsGloballyResponse.isSuccessful && existsGloballyResponse.body() == true) {
                val folderAnimalsResponse = RetrofitClient.apiService.getAnimalsByFolderId(folderId, "Bearer $token")
                val isInFolder = folderAnimalsResponse.isSuccessful &&
                        (folderAnimalsResponse.body()?.any { it.id == id } == true)

                val animalDetailsResponse = RetrofitClient.apiService.getAnimal(id, "Bearer $token")
                val animal = if (animalDetailsResponse.isSuccessful) animalDetailsResponse.body() else null

                updatedAnimals.add(
                    ScannedAnimalStatus(animal, id,
                        if (isInFolder) AnimalStatus.IN_FOLDER
                        else AnimalStatus.NOT_IN_FOLDER_EXISTS_GLOBALLY
                    )
                )
            } else {
                updatedAnimals.add(ScannedAnimalStatus(id = id, status = AnimalStatus.NEW_ANIMAL))
            }
        } catch (e: Exception) {
            Log.e("reloadAnimalsWithStatus", "Error processing $id: ${e.message}")
            updatedAnimals.add(ScannedAnimalStatus(id = id, status = AnimalStatus.NEW_ANIMAL))
        }
    }
    return updatedAnimals.sortedBy { it.id }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDialog(
    onDismiss: () -> Unit,
    onSave: (String, Map<String, Any>) -> Unit
) {
    var eventType by remember { mutableStateOf("") }
    var eventDate by remember { mutableStateOf("") }
    var field1 by remember { mutableStateOf("") }
    var field2 by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var eventTypes by remember { mutableStateOf<Map<String, Map<String, String>>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Fetch event types when dialog opens
    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.apiService.getEventTypes()
            if (response.isSuccessful) {
                eventTypes = response.body() ?: emptyMap()
                if (eventTypes.isNotEmpty()) {
                    eventType = eventTypes.keys.first()
                }
            } else {
                errorMessage = "Failed to load event types"
            }
        } catch (e: Exception) {
            errorMessage = "Error loading event types: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Animal Event") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                } else {
                    Text("Event Type:")
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = eventType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Event Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            eventTypes.forEach { (type, details) ->
                                DropdownMenuItem(
                                    text = { 
                                        Column {
                                            Text(type.capitalize())
                                            Text(
                                                details["description"] ?: "",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        eventType = type
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Show event type description
                    eventType.takeIf { it.isNotEmpty() }?.let { type ->
                        eventTypes[type]?.get("description")?.let { description ->
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = eventDate,
                        onValueChange = { eventDate = it },
                        label = { Text("Date (YYYY-MM-DD)") }
                    )

                    when (eventType) {
                        "vaccination" -> {
                            OutlinedTextField(
                                value = field1,
                                onValueChange = { field1 = it },
                                label = { Text("Vaccine Name") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = field2,
                                onValueChange = { field2 = it },
                                label = { Text("Dosage") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        "sickness" -> {
                            OutlinedTextField(
                                value = field1,
                                onValueChange = { field1 = it },
                                label = { Text("Diagnosis") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = field2,
                                onValueChange = { field2 = it },
                                label = { Text("Treatment") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        "birth" -> {
                            OutlinedTextField(
                                value = field1,
                                onValueChange = { field1 = it },
                                label = { Text("Calf Gender") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = field2,
                                onValueChange = { field2 = it },
                                label = { Text("Notes") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        "death" -> {
                            OutlinedTextField(
                                value = field1,
                                onValueChange = { field1 = it },
                                label = { Text("Cause of Death") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = field2,
                                onValueChange = { field2 = it },
                                label = { Text("Notes") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val details = mutableMapOf<String, Any>()
                    when (eventType) {
                        "vaccination" -> {
                            details["vaccineName"] = field1
                            details["dosage"] = field2
                        }
                        "sickness" -> {
                            details["diagnosis"] = field1
                            details["treatment"] = field2
                        }
                        "birth" -> {
                            details["calfGender"] = field1
                            details["notes"] = field2
                        }
                        "death" -> {
                            details["causeOfDeath"] = field1
                            details["notes"] = field2
                        }
                    }
                    details["eventType"] = eventType
                    details["eventDate"] = eventDate
                    onSave(eventType, details)
                },
                enabled = !isLoading && eventType.isNotEmpty() && eventDate.isNotEmpty() && 
                         field1.isNotEmpty() && field2.isNotEmpty()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
