package com.example.farmerappfrontend
import android.annotation.SuppressLint
import android.os.Build
import com.example.farmerappfrontend.Folder

import androidx.compose.material.icons.filled.Folder
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(navController: NavController, token: String) {
    var folderName by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isFileDialogOpen by remember { mutableStateOf(false) }
    var isGenerateFolderDialogOpen by remember { mutableStateOf(false) }
    var files by remember { mutableStateOf<List<Folder>>(emptyList()) }
    var selectedFolders by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedEventType by remember { mutableStateOf<EventType?>(null) }
    var selectedTimeRange by remember { mutableStateOf<TimeRange?>(null) }
    var selectedYear by remember { mutableStateOf(LocalDate.now().year.toString()) }
    var selectedEventName by remember { mutableStateOf("") }
    var eventTypes by remember { mutableStateOf<Map<String, Map<String, String>>>(emptyMap()) }
    var isEventTypesLoading by remember { mutableStateOf(true) }
    var eventTypesError by remember { mutableStateOf<String?>(null) }
    var sicknessNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var vaccineNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var isNamesLoading by remember { mutableStateOf(true) }
    var namesError by remember { mutableStateOf<String?>(null) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var editingFolderId by remember { mutableStateOf<String?>(null) }
    var editingFolderName by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    val context = navController.context

    // Fetch files from backend
    fun fetchFiles() {
        coroutineScope.launch {
            try {
                val response = RetrofitClient.apiService.getFolders("Bearer $token")
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
                    val folderRequest = FolderRequest(name = folderName)
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

    // Function to generate folder based on event criteria
    @SuppressLint("NewApi")
    fun generateFolderByEvents(
        eventType: EventType,
        timeRange: TimeRange? = null,
        year: String? = null,
        eventName: String? = null
    ) {
        coroutineScope.launch {
            try {
                // Create the folder first
                val folderName = when (eventType) {
                    EventType.BIRTH -> {
                        when (timeRange) {
                            TimeRange.LAST_3_MONTHS -> "Births Last 3 Months"
                            TimeRange.LAST_6_MONTHS -> "Births Last 6 Months"
                            TimeRange.SPECIFIC_YEAR -> "Births $year"
                            null -> "Births"
                        }
                    }
                    EventType.SICKNESS -> "Sickness: $eventName"
                    EventType.VACCINATION -> "Vaccination: $eventName"
                }
                val folderRequest = FolderRequest(name = folderName)
                val createResponse = RetrofitClient.apiService.createFolder("Bearer $token", folderRequest)
                if (!createResponse.isSuccessful) {
                    errorMessage = "Failed to create folder: ${createResponse.message()}"
                    return@launch
                }

                val newFolderId = createResponse.body()?.id ?: run {
                    errorMessage = "Failed to get new folder ID"
                    return@launch
                }

                // Fetch animals based on event criteria
                val animalsResponse = when (eventType) {
                    EventType.BIRTH -> {
                        val startDate = when (timeRange) {
                            TimeRange.LAST_3_MONTHS -> LocalDate.now().minus(3, ChronoUnit.MONTHS)
                            TimeRange.LAST_6_MONTHS -> LocalDate.now().minus(6, ChronoUnit.MONTHS)
                            TimeRange.SPECIFIC_YEAR -> LocalDate.of(year?.toIntOrNull() ?: LocalDate.now().year, 1, 1)
                            null -> LocalDate.now().minus(1, ChronoUnit.YEARS)
                        }
                        val endDate = when (timeRange) {
                            TimeRange.SPECIFIC_YEAR -> LocalDate.of(year?.toIntOrNull() ?: LocalDate.now().year, 12, 31)
                            else -> LocalDate.now()
                        }
                        RetrofitClient.apiService.getAnimalsByBirthDate(
                            startDate = startDate.format(DateTimeFormatter.ISO_DATE),
                            endDate = endDate.format(DateTimeFormatter.ISO_DATE),
                            token = "Bearer $token"
                        )
                    }
                    EventType.SICKNESS -> {
                        RetrofitClient.apiService.getAnimalsBySickness(
                            sicknessName = eventName ?: "",
                            token = "Bearer $token"
                        )
                    }
                    EventType.VACCINATION -> {
                        RetrofitClient.apiService.getAnimalsByVaccination(
                            vaccineName = eventName ?: "",
                            token = "Bearer $token"
                        )
                    }
                }

                if (!animalsResponse.isSuccessful) {
                    errorMessage = "Failed to fetch animals: ${animalsResponse.message()}"
                    return@launch
                }

                val animals = animalsResponse.body() ?: emptyList()

                // Add animals to the folder
                animals.forEach { animal ->
                    val addResponse = RetrofitClient.apiService.addAnimalToFolder(
                        folderId = newFolderId,
                        animalId = animal.id,
                        token = "Bearer $token"
                    )
                    if (!addResponse.isSuccessful) {
                        errorMessage = "Failed to add animal ${animal.id} to folder"
                    }
                }

                // Refresh the folder list
                fetchFiles()
            } catch (e: Exception) {
                errorMessage = "Error generating folder: ${e.message}"
            }
        }
    }

    // Function to refresh animals in a folder
    fun refreshFolderAnimals(folder: Folder) {
        coroutineScope.launch {
            try {
                val name = folder.name
                val response = when {
                    name.startsWith("Births Last 3 Months") -> {
                        val startDate = LocalDate.now().minusMonths(3).format(DateTimeFormatter.ISO_DATE)
                        val endDate = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
                        RetrofitClient.apiService.getAnimalsByBirthDate(startDate, endDate, "Bearer $token")
                    }
                    name.startsWith("Births Last 6 Months") -> {
                        val startDate = LocalDate.now().minusMonths(6).format(DateTimeFormatter.ISO_DATE)
                        val endDate = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
                        RetrofitClient.apiService.getAnimalsByBirthDate(startDate, endDate, "Bearer $token")
                    }
                    name.startsWith("Births ") -> {
                        val year = name.removePrefix("Births ").trim()
                        val startDate = LocalDate.of(year.toInt(), 1, 1).format(DateTimeFormatter.ISO_DATE)
                        val endDate = LocalDate.of(year.toInt(), 12, 31).format(DateTimeFormatter.ISO_DATE)
                        RetrofitClient.apiService.getAnimalsByBirthDate(startDate, endDate, "Bearer $token")
                    }
                    name.startsWith("Sickness:") -> {
                        val sicknessName = name.removePrefix("Sickness:").trim()
                        RetrofitClient.apiService.getAnimalsBySickness(sicknessName, "Bearer $token")
                    }
                    name.startsWith("Vaccination:") -> {
                        val vaccineName = name.removePrefix("Vaccination:").trim()
                        RetrofitClient.apiService.getAnimalsByVaccination(vaccineName, "Bearer $token")
                    }
                    else -> {
                        RetrofitClient.apiService.getAnimalsByFolderId(folder.id, "Bearer $token")
                    }
                }
                if (response.isSuccessful) {
                    val newAnimals = response.body() ?: emptyList()
                    // Fetch current animals in the folder
                    val currentResponse = RetrofitClient.apiService.getAnimalsByFolderId(folder.id, "Bearer $token")
                    val currentAnimalIds = currentResponse.body()?.map { it.id } ?: emptyList()
                    // Remove all current animals
                    if (currentAnimalIds.isNotEmpty()) {
                        RetrofitClient.apiService.removeAnimalsFromFolder(folder.id, currentAnimalIds, "Bearer $token")
                    }
                    // Add new animals
                    if (newAnimals.isNotEmpty()) {
                        RetrofitClient.apiService.addAnimalsToFolder(folder.id, newAnimals.map { it.id }, "Bearer $token")
                    }
                    toastMessage = "${folder.name}: ${newAnimals.size} animals"
                } else {
                    errorMessage = "Failed to refresh animals: ${response.message()}"
                }
            } catch (e: Exception) {
                errorMessage = "Error refreshing animals: ${e.message}"
            }
        }
    }

    // Function to check if a folder is generated (has specific naming patterns)
    fun isGeneratedFolder(folderName: String): Boolean {
        return folderName.startsWith("Births") || 
               folderName.startsWith("Sickness:") || 
               folderName.startsWith("Vaccination:")
    }

    // Function to rename a folder
    fun renameFolder(folderId: String, newName: String) {
        if (newName.isNotBlank()) {
            coroutineScope.launch {
                try {
                    val folderRequest = FolderRequest(name = newName)
                    val response = RetrofitClient.apiService.renameFolder(folderId, "Bearer $token", folderRequest)
                    if (response.isSuccessful) {
                        fetchFiles() // Refresh the list after renaming
                        editingFolderId = null
                        editingFolderName = ""
                    } else {
                        errorMessage = "Failed to rename folder: ${response.message()}"
                    }
                } catch (e: Exception) {
                    errorMessage = "Error renaming folder: ${e.message}"
                }
            }
        } else {
            errorMessage = "Folder name cannot be empty."
        }
    }

    // Function to handle double-click for renaming
    fun handleFolderClick(folder: Folder) {
        if (!isGeneratedFolder(folder.name)) {
            editingFolderId = folder.id
            editingFolderName = folder.name
        } else {
            navController.navigate("folder/${folder.id}/animals/$token")
        }
    }

    // Fetch files when the screen is first loaded
    LaunchedEffect(Unit) {
        fetchFiles()
    }

    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.apiService.getEventTypes()
            if (response.isSuccessful) {
                eventTypes = response.body() ?: emptyMap()
            } else {
                eventTypesError = "Failed to load event types"
            }
        } catch (e: Exception) {
            eventTypesError = "Error loading event types: ${e.message}"
        } finally {
            isEventTypesLoading = false
        }
    }

    LaunchedEffect(Unit) {
        try {
            val sicknessResponse = RetrofitClient.apiService.getSicknessNames("Bearer $token")
            val vaccineResponse = RetrofitClient.apiService.getVaccineNames("Bearer $token")
            if (sicknessResponse.isSuccessful && vaccineResponse.isSuccessful) {
                sicknessNames = sicknessResponse.body() ?: emptyList()
                vaccineNames = vaccineResponse.body() ?: emptyList()
            } else {
                namesError = "Failed to load sickness or vaccine names"
            }
        } catch (e: Exception) {
            namesError = "Error loading names: ${e.message}"
        } finally {
            isNamesLoading = false
        }
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
    @Composable
    fun TimeRangeSelector(
        selectedRange: TimeRange?,
        onRangeSelected: (TimeRange) -> Unit
    ) {
        Column {
            RadioButton(
                selected = selectedRange == TimeRange.LAST_3_MONTHS,
                onClick = { onRangeSelected(TimeRange.LAST_3_MONTHS) }
            )
            Text("Last 3 months")
            RadioButton(
                selected = selectedRange == TimeRange.LAST_6_MONTHS,
                onClick = { onRangeSelected(TimeRange.LAST_6_MONTHS) }
            )
            Text("Last 6 months")
            RadioButton(
                selected = selectedRange == TimeRange.SPECIFIC_YEAR,
                onClick = { onRangeSelected(TimeRange.SPECIFIC_YEAR) }
            )
            Text("Specific year")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "HerdID Logo",
                        modifier = Modifier.size(40.dp).
                        clickable{navController.navigate("home/$token")}
                    )
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

            // Top Buttons (Add File, Generate Folder)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { isFileDialogOpen = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Folder")
                }

                Button(
                    onClick = { isGenerateFolderDialogOpen = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Event, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Generate")
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
                            .padding(horizontal = 16.dp)
                            .pointerInput(folder.id) {
                                detectTapGestures(
                                    onTap = {
                                        navController.navigate("folder/${folder.id}/animals/$token")
                                    }
                                )
                            },
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
                                modifier = Modifier.weight(1f)
                            ) {
                                if (editingFolderId == folder.id) {
                                    Column {
                                        BasicTextField(
                                            value = editingFolderName,
                                            onValueChange = { editingFolderName = it },
                                            textStyle = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            ),
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            TextButton(
                                                onClick = {
                                                    if (editingFolderName.isNotBlank()) {
                                                        renameFolder(folder.id, editingFolderName)
                                                    }
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Save")
                                            }
                                            TextButton(
                                                onClick = {
                                                    editingFolderId = null
                                                    editingFolderName = ""
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Cancel")
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        folder.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.pointerInput(folder.id) {
                                            detectTapGestures(
                                                onDoubleTap = {
                                                    if (!isGeneratedFolder(folder.name)) {
                                                        editingFolderId = folder.id
                                                        editingFolderName = folder.name
                                                    }
                                                }
                                            )
                                        }
                                    )
                                }
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
                            
                            // Only show refresh button for generated folders
                            if (isGeneratedFolder(folder.name)) {
                                IconButton(onClick = { refreshFolderAnimals(folder) }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Refresh Animals")
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Delete Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { deleteFolders(selectedFolders) },
                    enabled = selectedFolders.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
            }

            // Show Toast for reload/refresh
            toastMessage?.let { msg ->
                LaunchedEffect(msg) {
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    toastMessage = null
                }
            }
        }
    }
    @Composable
    fun GenerateFolderDialog(
        onDismiss: () -> Unit,
        onGenerate: (EventType, TimeRange?, String?, String?) -> Unit
    ) {
        var selectedTab by remember { mutableStateOf(0) }
        val tabs = listOf("Birth", "Sickness", "Vaccination")
        val sicknessList = sicknessNames
        val vaccinationList = vaccineNames
        var expandedSickness by remember { mutableStateOf(false) }
        var expandedVaccination by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Generate Folder by Events") },
            text = {
                if (isNamesLoading) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (namesError != null) {
                    Text(namesError!!, color = MaterialTheme.colorScheme.error)
                } else {
                    Column {
                        TabRow(selectedTabIndex = selectedTab) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    text = { Text(title) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        when (selectedTab) {
                            0 -> {
                                Column {
                                    Text("Select time range for birth events:")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TimeRangeSelector(
                                        selectedRange = selectedTimeRange,
                                        onRangeSelected = { selectedTimeRange = it }
                                    )
                                    if (selectedTimeRange == TimeRange.SPECIFIC_YEAR) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        TextField(
                                            value = selectedYear,
                                            onValueChange = { selectedYear = it },
                                            label = { Text("Year") },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                            1 -> {
                                Column {
                                    Text("Select sickness type:")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    ExposedDropdownMenuBox(
                                        expanded = expandedSickness,
                                        onExpandedChange = { expandedSickness = !expandedSickness }
                                    ) {
                                        TextField(
                                            value = selectedEventName,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Sickness Type") },
                                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSickness) }
                                        )
                                        ExposedDropdownMenu(
                                            expanded = expandedSickness,
                                            onDismissRequest = { expandedSickness = false }
                                        ) {
                                            sicknessList.forEach { sickness ->
                                                DropdownMenuItem(
                                                    text = { Text(sickness) },
                                                    onClick = {
                                                        selectedEventName = sickness
                                                        expandedSickness = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            2 -> { // Vaccination events
                                Column {
                                    Text("Select vaccine type:")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    ExposedDropdownMenuBox(
                                        expanded = expandedVaccination,
                                        onExpandedChange = { expandedVaccination = !expandedVaccination }
                                    ) {
                                        TextField(
                                            value = selectedEventName,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Vaccine Type") },
                                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedVaccination) }
                                        )
                                        ExposedDropdownMenu(
                                            expanded = expandedVaccination,
                                            onDismissRequest = { expandedVaccination = false }
                                        ) {
                                            vaccinationList.forEach { vaccine ->
                                                DropdownMenuItem(
                                                    text = { Text(vaccine) },
                                                    onClick = {
                                                        selectedEventName = vaccine
                                                        expandedVaccination = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val eventType = when (selectedTab) {
                            0 -> EventType.BIRTH
                            1 -> EventType.SICKNESS
                            2 -> EventType.VACCINATION
                            else -> null
                        }
                        if (eventType != null) {
                            onGenerate(
                                eventType,
                                if (eventType == EventType.BIRTH) selectedTimeRange else null,
                                if (eventType == EventType.BIRTH && selectedTimeRange == TimeRange.SPECIFIC_YEAR) selectedYear else null,
                                if (eventType != EventType.BIRTH) selectedEventName else null
                            )
                        }
                        onDismiss()
                    }
                ) {
                    Text("Generate")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }



    if (isFileDialogOpen) {
        FileCreationDialog(
            onDismiss = { isFileDialogOpen = false },
            onCreate = { newFile ->
                createFolder(newFile)
            }
        )
    }

    if (isGenerateFolderDialogOpen) {
        GenerateFolderDialog(
            onDismiss = { 
                isGenerateFolderDialogOpen = false
                selectedEventType = null
                selectedTimeRange = null
                selectedYear = LocalDate.now().year.toString()
                selectedEventName = ""
            },
            onGenerate = { eventType, timeRange, year, eventName ->
                generateFolderByEvents(eventType, timeRange, year, eventName)
            }
        )
    }
}

enum class EventType {
    BIRTH, SICKNESS, VACCINATION
}

enum class TimeRange {
    LAST_3_MONTHS, LAST_6_MONTHS, SPECIFIC_YEAR
}