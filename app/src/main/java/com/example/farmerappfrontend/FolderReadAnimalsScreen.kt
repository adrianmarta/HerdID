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
import androidx.compose.foundation.Image
import androidx.navigation.NavController
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import com.example.farmerappfrontend.ui.components.EventDialog
import com.example.farmerappfrontend.components.SaveSessionDialog
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.ui.res.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderReadAnimalsScreen(
    token: String,
    folderId: String,
    navController: NavController,
    folderCameraViewModel: FolderCameraViewModel
) {
    val scannedAnimalIdsSet by folderCameraViewModel.scannedIds.collectAsState()
    val initialAnimalIds by folderCameraViewModel.initialScannedIds.collectAsState()
    val sessionId by folderCameraViewModel.sessionId.collectAsState()
    val sessionName by folderCameraViewModel.sessionName.collectAsState()

    val scannedAnimalIds = scannedAnimalIdsSet.toList()
    val isEditing = sessionId != null
    val isModified = remember(initialAnimalIds, scannedAnimalIdsSet) {
        initialAnimalIds != scannedAnimalIdsSet
    }

    var animalsWithStatus by remember { mutableStateOf<List<ScannedAnimalStatus>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var popupMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var selectedAnimalIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isShowingNotRead by remember { mutableStateOf(false) }
    var notReadAnimalIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var allAnimals by remember { mutableStateOf<List<AnimalDetails>>(emptyList()) }

    var selectAllInFolder by remember { mutableStateOf(false) }
    var selectAllNotInFolderExistsGlobally by remember { mutableStateOf(false) }
    var selectAllNewAnimal by remember { mutableStateOf(false) }
    var showSaveSessionDialog by remember { mutableStateOf(false) }

    var showEventDialog by remember { mutableStateOf(false) }

    // Fetch all animals
    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.apiService.getAnimalsByFolderId(folderId, "Bearer $token")
            if (response.isSuccessful) {
                allAnimals = response.body() ?: emptyList()
                notReadAnimalIds = allAnimals.map { it.id }.filterNot { scannedAnimalIds.contains(it) }
            }
        } catch (e: Exception) {
            popupMessage = "Error fetching folder animals: ${e.message}"
        }
    }

    // Update animal statuses
    LaunchedEffect(scannedAnimalIds, isShowingNotRead, allAnimals) {
        isLoading = true
        animalsWithStatus = if (isShowingNotRead) {
            reloadNotReadAnimals(folderId, notReadAnimalIds, token)
        } else {
            reloadAnimalsWithStatus(folderId, scannedAnimalIds, token)
        }
        isLoading = false
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
                            else "Read Animals (${scannedAnimalIds.size})"
                        )
                }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        val previousRoute = navController.previousBackStackEntry?.destination?.route
                        if (previousRoute?.startsWith("sessions") == true) {
                            scope.launch {
                                try {
                                    val response = RetrofitClient.apiService.getAnimalsByFolderId(folderId, "Bearer $token")
                                    if (response.isSuccessful) {
                                        val animalIds = response.body()?.map { it.id } ?: emptyList()
                                        val existingAnimalIdsString = animalIds.joinToString(",")
                                        navController.navigate("folderCamera/$token/$folderId?existingAnimalIds=$existingAnimalIdsString")
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
                // Top Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Add to Folder Button
                    Button(
                        onClick = {
                            val animalsToAdd = animalsWithStatus
                                .filter { it.id in selectedAnimalIds && it.status == AnimalStatus.NOT_IN_FOLDER_EXISTS_GLOBALLY }
                                .map { it.id }

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
                                            selectedAnimalIds = emptySet()
                                            isLoading = true
                                            animalsWithStatus = reloadAnimalsWithStatus(folderId, scannedAnimalIds, token)
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
                        enabled = selectedAnimalIds.isNotEmpty() &&
                                animalsWithStatus.filter { it.id in selectedAnimalIds }
                                    .all { it.status == AnimalStatus.NOT_IN_FOLDER_EXISTS_GLOBALLY }
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

                // Selection Checkboxes
                Column {
                    // Select Present
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
                    // Select Not Present
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
                    // Select New Animals
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

                Spacer(modifier = Modifier.height(16.dp))

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
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {

                                    val genderSymbol = if (item.animal?.gender?.equals("male", ignoreCase = true) == true) "♂" else if (item.animal?.gender?.equals("female", ignoreCase = true) == true) "♀" else ""
                                    Text("${item.id} $genderSymbol", style = MaterialTheme.typography.bodyLarge)

                                    item.animal?.birthDate?.let { birthDate ->
                                        Text(
                                            text = "Born: ${birthDate}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text("Status: ${when(item.status) { AnimalStatus.IN_FOLDER -> "Present"; AnimalStatus.NOT_IN_FOLDER_EXISTS_GLOBALLY -> "Not Present"; AnimalStatus.NEW_ANIMAL -> "New" }}", style = MaterialTheme.typography.bodyMedium)
                                }
                                if (item.status == AnimalStatus.NEW_ANIMAL) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(onClick = { navController.navigate("addSingleAnimal/$token/${item.id}") }) {
                                        Text("Add Animal ")
                                    }
                                }
                                else {
                                    IconButton(onClick = { navController.navigate("animalDetails/${item.id}/{token}") }) {
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

                // Bottom Action Buttons
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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

                                            // Refresh the list
                                            isLoading = true
                                            animalsWithStatus = if (isShowingNotRead) {
                                                allAnimals = allAnimals.filter { it.id !in animalIdsToDelete }
                                                notReadAnimalIds = allAnimals.map { it.id }.filterNot { scannedAnimalIds.contains(it) }
                                                reloadNotReadAnimals(folderId, notReadAnimalIds, token)
                                            } else {
                                                reloadAnimalsWithStatus(folderId, scannedAnimalIds, token)
                                            }
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

                    if (isEditing) {
                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        val request = CountingSessionRequest(
                                            name = sessionName ?: "Unnamed",
                                            folderId = folderId,
                                            readAnimalIds = scannedAnimalIds
                                        )
                                        val response = RetrofitClient.apiService.updateCountingSession(sessionId!!, "Bearer $token", request)
                                        if (response.isSuccessful) {
                                            popupMessage = "Session updated successfully."
                                            folderCameraViewModel.updateInitialState()
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
                            enabled = scannedAnimalIds.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Save Session")
                        }
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

    if (showSaveSessionDialog) {
        SaveSessionDialog(
            onDismiss = { showSaveSessionDialog = false },
            onSave = { newSessionName ->
                scope.launch {
                    try {
                        val request = CountingSessionRequest(
                            name = newSessionName,
                            folderId = folderId,
                            readAnimalIds = scannedAnimalIds
                        )
                        val response = RetrofitClient.apiService.saveCountingSession("Bearer $token", request)
                        if (response.isSuccessful) {
                            val savedSession = response.body()
                            if (savedSession != null && savedSession.id.isNotBlank()) {
                                popupMessage = "Session '$newSessionName' saved successfully."
                                showSaveSessionDialog = false
                                folderCameraViewModel.setSessionIdAndName(savedSession.id, newSessionName)
                                folderCameraViewModel.updateInitialState()
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

    // Event Dialog
    if (showEventDialog) {
        EventDialog(
            token = token,
            animalIds = selectedAnimalIds.toList(),
            onDismiss = { showEventDialog = false },
            onSuccess = {
                popupMessage = "Event posting complete."
                showEventDialog = false
                selectedAnimalIds = emptySet()
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
                val isInFolder = folderAnimalsResponse.isSuccessful && (folderAnimalsResponse.body()?.any { it.id == id } == true)
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

suspend fun reloadNotReadAnimals(
    folderId: String,
    notReadAnimalIds: List<String>,
    token: String
): List<ScannedAnimalStatus> {
    val results = mutableListOf<ScannedAnimalStatus>()
    for (id in notReadAnimalIds) {
        try {
            val animalDetailsResponse = RetrofitClient.apiService.getAnimal(id, "Bearer $token")
            if (animalDetailsResponse.isSuccessful) {
                results.add(ScannedAnimalStatus(animalDetailsResponse.body(), id, AnimalStatus.IN_FOLDER))
            } else {
                results.add(ScannedAnimalStatus(id = id, status = AnimalStatus.IN_FOLDER))
            }
        } catch (e: Exception) {
            Log.e("reloadNotReadAnimals", "Error processing $id: ${e.message}")
            results.add(ScannedAnimalStatus(id = id, status = AnimalStatus.IN_FOLDER))
        }
    }
    return results.sortedBy { it.id }
}



