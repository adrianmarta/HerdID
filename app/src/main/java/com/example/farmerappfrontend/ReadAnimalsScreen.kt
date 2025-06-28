package com.example.farmerappfrontend

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.farmerappfrontend.ui.components.EventDialog
import kotlinx.coroutines.launch
import com.example.farmerappfrontend.components.SaveSessionDialog
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.ui.res.painterResource


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadAnimalsScreen(
    token: String,
    navController: NavController,
    cameraViewModel: CameraViewModel
) {
    val scope = rememberCoroutineScope()
    val readAnimalIdsSet by cameraViewModel.scannedIds.collectAsState()
    val initialAnimalIds by cameraViewModel.initialScannedIds.collectAsState()
    val sessionId by cameraViewModel.sessionId.collectAsState()
    val sessionName by cameraViewModel.sessionName.collectAsState()

    val readAnimalIds = readAnimalIdsSet.toList()
    val isEditing = sessionId != null
    val isModified = remember(initialAnimalIds, readAnimalIdsSet) {
        initialAnimalIds != readAnimalIdsSet
    }

    var selectedAnimals by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var showEventDialog by remember { mutableStateOf(false) }
    var showAddNewAnimalsDialog by remember { mutableStateOf(false) }
    var popupMessage by remember { mutableStateOf<String?>(null) }
    var existingAnimalDetails by remember { mutableStateOf<List<AnimalDetails>>(emptyList()) }
    var isLoadingDetails by remember { mutableStateOf(true) }
    var isShowingNotRead by remember { mutableStateOf(false) }
    var notReadAnimalIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var allAnimals by remember { mutableStateOf<List<AnimalDetails>>(emptyList()) }
    var newAnimalIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var showSaveSessionDialog by remember { mutableStateOf(false) }


    var selectAllPresent by remember { mutableStateOf(false) }
    var selectAllNew by remember { mutableStateOf(false) }

    LaunchedEffect(token, readAnimalIds) {
        try {
                val response = RetrofitClient.apiService.getAnimalsByOwnerId("Bearer $token")
                if (response.isSuccessful) {
                    allAnimals = response.body() ?: emptyList()
                    val allAnimalIds = allAnimals.map { it.id }.toSet()
                    newAnimalIds = readAnimalIds.filterNot { allAnimalIds.contains(it) }
                    notReadAnimalIds = allAnimals.map { it.id }.filterNot { readAnimalIds.contains(it) }
                }
        } catch (e: Exception) {
            popupMessage = "Error fetching user data: ${e.message}"
        }
    }

    // Update animal details
    LaunchedEffect(isShowingNotRead, allAnimals, readAnimalIds, newAnimalIds) {
        isLoadingDetails = true
        try {
            if (isShowingNotRead) {
                existingAnimalDetails = allAnimals.filter { it.id in notReadAnimalIds }
            } else {
                val existingIds = readAnimalIds.filter { !newAnimalIds.contains(it) }
                if (existingIds.isNotEmpty()) {
                    val response = RetrofitClient.apiService.getAnimalsByIds(existingIds, "Bearer $token")
                    if (response.isSuccessful) {
                        existingAnimalDetails = response.body() ?: emptyList()
                    }
                } else {
                    existingAnimalDetails = emptyList()
                }
            }
        } catch (e: Exception) {
            popupMessage = "Error fetching animal details: ${e.message}"
        } finally {
            isLoadingDetails = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "HerdID Logo",
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { navController.navigate("home/$token") },
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        if (isShowingNotRead) "Not Read Animals (${notReadAnimalIds.size})" 
                        else "Read Animals (${readAnimalIds.size})"
                    )
                        }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        val previousRoute = navController.previousBackStackEntry?.destination?.route
                        if (previousRoute?.startsWith("sessions") == true) {
                            scope.launch {
                                try {
                                    val response = RetrofitClient.apiService.getAnimalsByOwnerId("Bearer $token")
                                    if (response.isSuccessful) {
                                        val animalIds = response.body()?.map { it.id } ?: emptyList()
                                        val existingAnimalIdsString = animalIds.joinToString(",")
                                        navController.navigate("camera/$token/$existingAnimalIdsString")
                                    } else {
                                        navController.popBackStack()
                                    }
                                } catch (e: Exception) {
                                    navController.popBackStack()
                                }
                            }
                        } else {
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        navController.navigate("home/$token") {
                            popUpTo("home/$token") { inclusive = true }
                        }
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Exit")
                    }
                    IconButton(
                        onClick = { 
                            isShowingNotRead = !isShowingNotRead
                            selectedAnimals = emptySet()
                        }
                    ) {
                        Icon(
                            if (isShowingNotRead) Icons.Default.CheckCircle else Icons.Default.List,
                            contentDescription = if (isShowingNotRead) "Show Read Animals" else "Show Not Read Animals"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(MaterialTheme.colorScheme.primary)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showFolderDialog = true },
                    modifier = Modifier.weight(1f),
                    enabled = selectedAnimals.isNotEmpty()
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add to Folder")
                }

                Button(
                    onClick = { showEventDialog = true },
                    modifier = Modifier.weight(1f),
                    enabled = selectedAnimals.isNotEmpty()
                ) {
                    Icon(Icons.Default.Event, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Event")
                }
            }

            // Selection Checkboxes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Select Present
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Checkbox(
                        checked = selectAllPresent,
                        onCheckedChange = { checked ->
                            selectAllPresent = checked
                            val presentIds = if (isShowingNotRead) {
                                notReadAnimalIds.toSet()
                            } else {
                                readAnimalIds.filterNot { newAnimalIds.contains(it) }.toSet()
                            }
                            selectedAnimals = if (checked) {
                                selectedAnimals + presentIds
                            } else {
                                selectedAnimals - presentIds
                            }
                        }
                    )
                    Text(
                        "Select Present (${if (isShowingNotRead) notReadAnimalIds.size else readAnimalIds.count { !newAnimalIds.contains(it) }})",
                        modifier = Modifier.clickable { selectAllPresent = !selectAllPresent }
                    )
                }

                // Select New Animals
                if (!isShowingNotRead) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Checkbox(
                            checked = selectAllNew,
                            onCheckedChange = { checked ->
                                selectAllNew = checked
                                val newIds = newAnimalIds.toSet()
                                selectedAnimals = if (checked) {
                                    selectedAnimals + newIds
                                } else {
                                    selectedAnimals - newIds
                                }
                            }
                        )
                        Text(
                            "Select New (${newAnimalIds.size})",
                            modifier = Modifier.clickable { selectAllNew = !selectAllNew }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Animal list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(if (isShowingNotRead) notReadAnimalIds else readAnimalIds) { animalId ->
                    val isNew = !isShowingNotRead && newAnimalIds.contains(animalId)
                    val animalDetails = existingAnimalDetails.find { it.id == animalId }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedAnimals = if (selectedAnimals.contains(animalId)) {
                                    selectedAnimals - animalId
                                } else {
                                    selectedAnimals + animalId
                                }
                            },
                        elevation = CardDefaults.elevatedCardElevation(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedAnimals.contains(animalId)) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = if (animalDetails != null) {
                                        val genderSymbol = if (animalDetails.gender.equals("male", ignoreCase = true)) "♂" else "♀"
                                        "$animalId $genderSymbol"
                                    } else {
                                        animalId
                                    },
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                if (animalDetails != null) {
                                    Text(
                                        text = "Born: ${animalDetails.birthDate}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isNew) {
                                    Text(
                                        text = "New Animal",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                            if (isNew) {

                                Button(
                                    onClick = { navController.navigate("addSingleAnimal/$token/$animalId") },
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Text("Add")
                                }
                            }
                                else {
                                    IconButton(onClick = { navController.navigate("animalDetails/${animalId}/{token}") }) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "Details"
                                        )
                                    }
                                }

                            if (selectedAnimals.contains(animalId)) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Bottom buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                selectedAnimals.forEach { animalId ->
                                    RetrofitClient.apiService.deleteAnimal(animalId, "Bearer $token")
                                }
                                popupMessage = "Selected animals deleted successfully"
                                selectedAnimals = emptySet()
                                
                                if (isShowingNotRead) {
                                    allAnimals = allAnimals.filter { it.id !in selectedAnimals }
                                    notReadAnimalIds = allAnimals.map { it.id }.filterNot { readAnimalIds.contains(it) }
                                    existingAnimalDetails = allAnimals.filter { it.id in notReadAnimalIds }
                                } else {
                                    val existingIds = readAnimalIds.filter { !newAnimalIds.contains(it) && it !in selectedAnimals }
                                    if (existingIds.isNotEmpty()) {
                                        val response = RetrofitClient.apiService.getAnimalsByIds(existingIds, "Bearer $token")
                                        if (response.isSuccessful) {
                                            existingAnimalDetails = response.body() ?: emptyList()
                                        }
                                    } else {
                                        existingAnimalDetails = emptyList()
                                    }
                                }
                            } catch (e: Exception) {
                                popupMessage = "Error deleting animals: ${e.message}"
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    enabled = selectedAnimals.isNotEmpty()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Delete Selected")
                }

                if (isEditing) {
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    val request = CountingSessionRequest(
                                        name = sessionName ?: "Unnamed",
                                        folderId = null,
                                        readAnimalIds = readAnimalIds
                                    )
                                    val response = RetrofitClient.apiService.updateCountingSession(sessionId!!, "Bearer $token", request)
                                    if (response.isSuccessful) {
                                        popupMessage = "Session updated successfully."
                                        cameraViewModel.updateInitialState()
                                    } else {
                                        popupMessage = "Failed to update session: ${response.message()}"
                                    }
                                } catch (e: Exception) {
                                    popupMessage = "Error: ${e.message}"
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = isModified
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Update Session")
                    }
                } else {
                    Button(
                        onClick = { showSaveSessionDialog = true },
                        modifier = Modifier.weight(1f),
                        enabled = readAnimalIds.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Save Session")
                    }
                }
            }

            // Update checkbox
            LaunchedEffect(selectedAnimals, isShowingNotRead, newAnimalIds) {
                if (isShowingNotRead) {
                    selectAllPresent = notReadAnimalIds.isNotEmpty() && selectedAnimals.containsAll(notReadAnimalIds)
                    selectAllNew = false
                } else {
                    val presentIds = readAnimalIds.filterNot { newAnimalIds.contains(it) }.toSet()
                    selectAllPresent = presentIds.isNotEmpty() && selectedAnimals.containsAll(presentIds)
                    selectAllNew = newAnimalIds.isNotEmpty() && selectedAnimals.containsAll(newAnimalIds)
                }
            }

            // Update checkbox
            LaunchedEffect(isShowingNotRead) {
                selectedAnimals = emptySet()
                selectAllPresent = false
                selectAllNew = false
            }
        }
    }

    // Folder dialog
    if (showFolderDialog) {
        FolderSelectionDialog(
            token = token,
            selectedAnimals = selectedAnimals.toList(),
            onDismiss = { showFolderDialog = false },
            onSuccess = {
                popupMessage = "Animals added to folder successfully"
                showFolderDialog = false
            }
        )
    }

    // Event dialog
    if (showEventDialog) {
        EventDialog(
            token = token,
            animalIds = selectedAnimals.toList(),
            onDismiss = { showEventDialog = false },
            onSuccess = {
                popupMessage = "Event added successfully"
                showEventDialog = false
            }
        )
    }

    if (showSaveSessionDialog) {
        SaveSessionDialog(
            onDismiss = { showSaveSessionDialog = false },
            onSave = { newSessionName ->
                scope.launch {
                    try {
                        val request = CountingSessionRequest(
                            name = newSessionName,
                            folderId = null,
                            readAnimalIds = readAnimalIds
                        )
                        val response = RetrofitClient.apiService.saveCountingSession("Bearer $token", request)
                        if (response.isSuccessful) {
                            val savedSession = response.body()
                            if (savedSession != null && savedSession.id.isNotBlank()) {
                                popupMessage = "Session '$newSessionName' saved successfully."
                                showSaveSessionDialog = false
                                cameraViewModel.setSessionIdAndName(savedSession.id, newSessionName)
                                cameraViewModel.updateInitialState()
                            } else {
                                popupMessage = "Failed to save session: Server did not return a session object."
                            }
                        } else {
                            popupMessage = "Failed to save session: ${response.message()}"
                        }
                    } catch (e: Exception) {
                        popupMessage = "Error: ${e.message}"
                    }
                }
            }
        )
    }

    // Popup messages
    popupMessage?.let {
        AlertDialog(
            onDismissRequest = { popupMessage = null },
            title = { Text("Notification") },
            text = { Text(it) },
            confirmButton = {
                Button(onClick = { popupMessage = null }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun FolderSelectionDialog(
    token: String,
    selectedAnimals: List<String>,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var folders by remember { mutableStateOf<List<Folder>>(emptyList()) }
    var selectedFolderId by remember { mutableStateOf<String?>(null) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            isLoading = true

                val response = RetrofitClient.apiService.getFolders("Bearer $token")
                if (response.isSuccessful) {
                    folders = response.body() ?: emptyList()
                } else {
                    errorMessage = "Failed to load folders"
                }

        } catch (e: Exception) {
            errorMessage = "Error loading folders: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Folder") },
        text = {
            Column {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                } else {
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Button(
                        onClick = { showCreateFolderDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreateNewFolder,
                            contentDescription = "Create New Folder"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create New Folder")
                    }

                    LazyColumn {
                        items(folders) { folder ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        selectedFolderId = if (selectedFolderId == folder.id) null else folder.id
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedFolderId == folder.id) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    }
                                )
                            ) {
                                Text(
                                    text = folder.name,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onDismiss
                ) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        selectedFolderId?.let { folderId ->
                            scope.launch {
                                try {
                                    isLoading = true
                                    val response = RetrofitClient.apiService.addAnimalsToFolder(
                                        folderId = folderId,
                                        animalIds = selectedAnimals,
                                        authorization = "Bearer $token"
                                    )
                                    if (response.isSuccessful) {
                                        onSuccess()
                                    } else {
                                        errorMessage = "Failed to add animals to folder"
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Error: ${e.message}"
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    },
                    enabled = selectedFolderId != null && selectedAnimals.isNotEmpty()
                ) {
                    Text("Add to Folder")
                }
            }
        }
    )

    // Create New Folder Dialog
    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { 
                showCreateFolderDialog = false
                newFolderName = ""
                errorMessage = null
            },
            title = { Text("Create New Folder") },
            text = {
                Column {
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        label = { Text("Folder Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { showCreateFolderDialog = false }
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (newFolderName.isNotBlank()) {
                                scope.launch {
                                    try {
                                        isLoading = true


                                            val response = RetrofitClient.apiService.createFolder(
                                                token = "Bearer $token",
                                                folderRequest = FolderRequest(
                                                    name = newFolderName

                                                )
                                            )
                                            if (response.isSuccessful) {
                                                val foldersResponse = RetrofitClient.apiService.getFolders("Bearer $token")
                                                if (foldersResponse.isSuccessful) {
                                                    folders = foldersResponse.body() ?: emptyList()
                                                }
                                                showCreateFolderDialog = false
                                                newFolderName = ""
                                            } else {
                                                errorMessage = "Failed to create folder"
                                            }

                                    } catch (e: Exception) {
                                        errorMessage = "Error: ${e.message}"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        },
                        enabled = newFolderName.isNotBlank()
                    ) {
                        Text("Create")
                    }
                }
            }
        )
    }
}

