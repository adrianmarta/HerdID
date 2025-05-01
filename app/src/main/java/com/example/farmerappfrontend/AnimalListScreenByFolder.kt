package com.example.farmerappfrontend

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimalListScreenByFolder(
    token: String,
    folderId: String,
    navController: NavController,
    ownerId: String
) {
    var animals by remember { mutableStateOf<List<Animal>>(emptyList()) }
    var filteredAnimals by remember { mutableStateOf<List<Animal>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedAnimals by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isCounting by remember { mutableStateOf(false) }
    var isCameraOpen by remember { mutableStateOf(false) }
    var isAddByIdOpen by remember { mutableStateOf(false) }
    var manualIdInput by remember { mutableStateOf("") }
    var isCooldownActive by remember { mutableStateOf(false) }
    var popupMessage by remember { mutableStateOf<String?>(null) }
    var isAddEventDialogOpen by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(folderId) {
        fetchAnimalsInFolder(folderId, token) {
            animals = it
            filteredAnimals = it
        }
    }

    LaunchedEffect(searchQuery) {
        filteredAnimals = if (searchQuery.isEmpty()) {
            animals
        } else {
            animals.filter {
                it.id.contains(searchQuery, ignoreCase = true) ||
                        it.birthDate.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Animals in Folder") },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(MaterialTheme.colorScheme.primary)
                )
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search animals...") },
                    leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = "Search") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
                TopActionButtons(
                    isCameraOpenSetter = { isCameraOpen = true },
                    isAddByIdOpenSetter = { isAddByIdOpen = true }
                )
                if (selectedAnimals.isNotEmpty()) {
                    SelectedActionButtons(
                        onDeleteClick = {
                            deleteSelectedAnimalsFromFolders(folderId, selectedAnimals.toList(), token) {
                                fetchAnimalsInFolder(folderId, token) {
                                    animals = it
                                    filteredAnimals = it
                                    selectedAnimals = emptySet()
                                }
                            }
                        },
                        onAddEventClick = { isAddEventDialogOpen = true }
                    )
                }
            }
        },
        bottomBar = {
            BottomActionButtons(
                onSelectAllClick = { selectedAnimals = filteredAnimals.map { it.id }.toSet() },
                onCountClick = { isCounting = true }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredAnimals) { animal ->
                AnimalRowWithSelection(
                    animal = animal,
                    isSelected = selectedAnimals.contains(animal.id),
                    onToggleSelection = { animalId ->
                        selectedAnimals = if (selectedAnimals.contains(animalId)) {
                            selectedAnimals - animalId
                        } else {
                            selectedAnimals + animalId
                        }
                    },
                    onDetailsClick = {
                        navController.navigate("animalDetails/${animal.id}")
                    }
                )
            }
        }
    }

    if (isAddByIdOpen) {
        AddAnimalByIdDialog(
            manualIdInput = manualIdInput,
            onInputChange = { manualIdInput = it },
            onConfirm = {
                CoroutineScope(Dispatchers.IO).launch {
                    handleScannedId(
                        token = token,
                        folderId = folderId,
                        scannedId = manualIdInput.trim(),
                        currentAnimals = animals,
                        onResult = {
                            popupMessage = it
                            isAddByIdOpen = false
                        }
                    )
                    fetchAnimalsInFolder(folderId, token) {
                        animals = it
                        filteredAnimals = it
                    }
                }
            },
            onDismiss = { isAddByIdOpen = false }
        )
    }

    if (isAddEventDialogOpen) {
        AddEventToAllDialog(
            token = token,
            animalIds = selectedAnimals.toList(),
            onDismiss = { isAddEventDialogOpen = false },
            onComplete = {
                popupMessage = "Event added to selected animals!"
                selectedAnimals = emptySet()
                isAddEventDialogOpen = false
            }
        )
    }

    if (popupMessage != null) {
        AlertDialog(
            onDismissRequest = { popupMessage = null },
            title = { Text("Info") },
            text = { Text(popupMessage ?: "") },
            confirmButton = {
                Button(onClick = { popupMessage = null }) {
                    Text("OK")
                }
            }
        )
    }

    if (isCounting) {
        CountingCamera(
            token = token,
            animalIds = animals.map { it.id },
            onComplete = { idsNotRead ->
                isCounting = false
                navController.navigate("notReadAnimals/$token/${idsNotRead.joinToString(",")}/$ownerId?folderId=$folderId")
            }
        )
    }

    if (isCameraOpen) {
        CameraIDReader(
            onIDDetected = { scannedId ->
                if (scannedId != null && !isCooldownActive) {
                    isCooldownActive = true
                    scope.launch {
                        handleScannedId(
                            token = token,
                            folderId = folderId,
                            scannedId = scannedId,
                            currentAnimals = animals,
                            onResult = { popupMessage = it }
                        )
                        fetchAnimalsInFolder(folderId, token) {
                            animals = it
                            filteredAnimals = it
                        }
                        kotlinx.coroutines.delay(5000)
                        isCooldownActive = false
                    }
                }
            },
            onError = { error -> popupMessage = "Error: $error" },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun TopActionButtons(
    isCameraOpenSetter: () -> Unit,
    isAddByIdOpenSetter: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = isCameraOpenSetter,
            modifier = Modifier.weight(1f)
        ) {
            Text("Add via Camera")
        }
        Button(
            onClick = isAddByIdOpenSetter,
            modifier = Modifier.weight(1f)
        ) {
            Text("Add by ID")
        }
    }
}

@Composable
fun SelectedActionButtons(
    onDeleteClick: () -> Unit,
    onAddEventClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onDeleteClick,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.weight(1f)
        ) {
            Text("Delete Selected")
        }
        Button(
            onClick = onAddEventClick,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            modifier = Modifier.weight(1f)
        ) {
            Text("Add Event")
        }
    }
}

@Composable
fun BottomActionButtons(
    onSelectAllClick: () -> Unit,
    onCountClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onSelectAllClick,
            modifier = Modifier.weight(1f)
        ) {
            Text("Select All")
        }
        Button(
            onClick = onCountClick,
            modifier = Modifier.weight(1f)
        ) {
            Text("Count")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventToAllDialog(
    token: String,
    animalIds: List<String>,
    onDismiss: () -> Unit,
    onComplete: () -> Unit
) {
    var eventType by remember { mutableStateOf("vaccination") }
    var eventDate by remember { mutableStateOf("") }
    var field1 by remember { mutableStateOf("") }
    var field2 by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Event to All Animals") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Event Type:")
                Box {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = eventType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Event Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            listOf("vaccination", "sickness", "birth").forEach { selection ->
                                DropdownMenuItem(
                                    text = { Text(selection) },
                                    onClick = {
                                        eventType = selection
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = eventDate,
                    onValueChange = { eventDate = it },
                    label = { Text("Event Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                when (eventType) {
                    "vaccination" -> {
                        OutlinedTextField(value = field1, onValueChange = { field1 = it }, label = { Text("Vaccine Name") })
                        OutlinedTextField(value = field2, onValueChange = { field2 = it }, label = { Text("Dosage") })
                    }
                    "sickness" -> {
                        OutlinedTextField(value = field1, onValueChange = { field1 = it }, label = { Text("Diagnosis") })
                        OutlinedTextField(value = field2, onValueChange = { field2 = it }, label = { Text("Treatment") })
                    }
                    "birth" -> {
                        OutlinedTextField(value = field1, onValueChange = { field1 = it }, label = { Text("Calf Gender") })
                        OutlinedTextField(value = field2, onValueChange = { field2 = it }, label = { Text("Notes") })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val details = mutableMapOf<String, Any>(
                    "eventType" to eventType,
                    "eventDate" to eventDate
                )
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
                }
                postEventToAllAnimals(token, animalIds, details) {
                    onComplete()
                }
            }) {
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

@Composable
fun AnimalRowWithSelection(
    animal: Animal,
    isSelected: Boolean,
    onToggleSelection: (String) -> Unit,
    onDetailsClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onToggleSelection(animal.id) },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = if (isSelected) {
            CardDefaults.cardColors(containerColor = Color(0xFF90CAF9))
        } else {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val genderSymbol = if (animal.gender.equals("male", ignoreCase = true)) "♂" else "♀"
                Text("${animal.id} $genderSymbol", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "Born: ${animal.birthDate}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDetailsClick) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Details"
                    )
                }
            }
        }
    }
}

@Composable
fun AddAnimalByIdDialog(
    manualIdInput: String,
    onInputChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Animal by ID") },
        text = {
            Column {
                TextField(
                    value = manualIdInput,
                    onValueChange = onInputChange,
                    label = { Text("Animal ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text("Add") }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

fun fetchAnimalsInFolder(folderId: String, token: String, onResult: (List<Animal>) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = RetrofitClient.apiService.getAnimalsByFolderId(folderId, "Bearer $token")
            if (response.isSuccessful) {
                onResult(response.body()?.filterNotNull() ?: emptyList())
            } else {
                onResult(emptyList())
            }
        } catch (e: Exception) {
            onResult(emptyList())
        }
    }
}

suspend fun handleScannedId(
    token: String,
    folderId: String,
    scannedId: String,
    currentAnimals: List<Animal>,
    onResult: (String) -> Unit
) {
    try {
        val globalResponse = RetrofitClient.apiService.checkAnimalExists(scannedId, "Bearer $token")
        if (!globalResponse.isSuccessful || globalResponse.body() != true) {
            onResult("The ID is not present in the global animal list.")
            return
        }

        if (currentAnimals.any { it.id == scannedId }) {
            onResult("This animal is already in the folder.")
            return
        }

        val addResponse = RetrofitClient.apiService.addAnimalToFolder(folderId, scannedId, "Bearer $token")
        if (addResponse.isSuccessful) {
            onResult("Animal added successfully.")
        } else {
            onResult("Failed to add animal to folder: ${addResponse.message()} (Code: ${addResponse.code()})")
        }
    } catch (e: Exception) {
        onResult("Error: ${e.message}")
    }
}

fun deleteSelectedAnimalsFromFolders(
    folderId: String,
    animalIds: List<String>,
    token: String,
    onComplete: () -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = RetrofitClient.apiService.removeAnimalsFromFolder(folderId, animalIds, "Bearer $token")
            if (response.isSuccessful) {
                Log.d("DeleteAnimals", "Animals successfully removed from folder: $folderId")
                onComplete()
            } else {
                Log.e("DeleteAnimals", "Failed to remove animals: ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("DeleteAnimals", "Error removing animals from folder: ${e.message}")
        }
    }
}

fun postEventToAllAnimals(token: String, animalIds: List<String>, eventDetails: Map<String, Any>, onComplete: () -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            animalIds.forEach { animalId ->
                val response = RetrofitClient.apiService.postEvent(animalId, "Bearer $token", eventDetails)
                if (!response.isSuccessful) {
                    Log.e("AddEventAll", "Failed to add event to animal $animalId: ${response.message()}")
                }
            }
            onComplete()
        } catch (e: Exception) {
            Log.e("AddEventAll", "Error adding events: ${e.message}")
        }
    }
}