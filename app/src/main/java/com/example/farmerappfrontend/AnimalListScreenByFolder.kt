package com.example.farmerappfrontend

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.net.Uri
import androidx.compose.foundation.Image
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.util.Matrix
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.OutputStream
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.ui.res.painterResource
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import androidx.compose.material3.RangeSlider
import android.annotation.SuppressLint

@SuppressLint("NewApi")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimalListScreenByFolder(
    token: String,
    folderId: String,
    navController: NavController,
    folderCameraViewModel: FolderCameraViewModel
) {
    var animals by remember { mutableStateOf<List<AnimalDetails>>(emptyList()) }
    var filteredAnimals by remember { mutableStateOf<List<AnimalDetails>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedAnimals by remember { mutableStateOf<Set<String>>(emptySet()) }
    var sortOrder by remember { mutableStateOf<SortOrder>(SortOrder.NONE) }
    var showAddByIdDialog by remember { mutableStateOf(false) }
    var showAddEventDialog by remember { mutableStateOf(false) }
    var addByIdInput by remember { mutableStateOf("") }
    var matchingAnimals by remember { mutableStateOf<List<AnimalDetails>>(emptyList()) }
    var isExporting by remember { mutableStateOf(false) }
    var exportMessage by remember { mutableStateOf<String?>(null) }
    var folderName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var popupMessage by remember { mutableStateOf<String?>(null) }
    var isAddingEvents by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    var filterSex by remember { mutableStateOf<String?>(null) }
    var filterBirthDateFrom by remember { mutableStateOf("") }
    var filterBirthDateTo by remember { mutableStateOf("") }
    var birthDateRange by remember { mutableStateOf(0f..1f) }
    var filterSpecies by remember { mutableStateOf<Set<String>>(emptySet()) }
    var filterMilkYes by remember { mutableStateOf(false) }
    var filterMilkNo by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val allSpecies = animals.map { it.species }.distinct().sorted()

    val dateRange = remember(animals) {
        if (animals.isEmpty()) {
            Pair(LocalDate.now(), LocalDate.now())
        } else {
            val dates = animals.mapNotNull {
                try {
                    LocalDate.parse(it.birthDate)
                } catch (e: Exception) {
                    null
                }
            }
            if (dates.isEmpty()) {
                Pair(LocalDate.now(), LocalDate.now())
            } else {
                Pair(dates.minOrNull()!!, LocalDate.now())
            }
        }
    }

    fun sliderValueToDate(value: Float): LocalDate {
        val daysBetween = ChronoUnit.DAYS.between(dateRange.first, dateRange.second)
        val daysToAdd = (daysBetween * value).toLong()
        return dateRange.first.plusDays(daysToAdd)
    }

    fun dateToSliderValue(date: LocalDate): Float {
        val daysBetween = ChronoUnit.DAYS.between(dateRange.first, dateRange.second)
        val daysFromStart = ChronoUnit.DAYS.between(dateRange.first, date)
        return if (daysBetween > 0) daysFromStart.toFloat() / daysBetween else 0f
    }

    fun isValidDateString(dateStr: String): Boolean {
        return try {
            LocalDate.parse(dateStr)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun dateStringToSliderValue(dateStr: String): Float {
        return try {
            val date = LocalDate.parse(dateStr)
            dateToSliderValue(date)
        } catch (e: Exception) {
            0f
        }
    }

    fun updateBirthDateRange(startDate: String, endDate: String) {
        if (isValidDateString(startDate) && isValidDateString(endDate)) {
            val start = dateStringToSliderValue(startDate)
            val end = dateStringToSliderValue(endDate)
            if (start <= end) {
                birthDateRange = start..end
            }
        }
    }

    LaunchedEffect(birthDateRange) {
        filterBirthDateFrom = sliderValueToDate(birthDateRange.start).format(DateTimeFormatter.ISO_DATE)
        filterBirthDateTo = sliderValueToDate(birthDateRange.endInclusive).format(DateTimeFormatter.ISO_DATE)
    }

    // Adaugă funcția pentru filtrare după eveniment
    suspend fun filterAnimalsByEventPeriod(eventType: String, startDate: String, endDate: String, token: String): List<AnimalDetails> {
        val response = RetrofitClient.apiService.getAnimalsByEventTypeAndDate(
            eventType = eventType,
            startDate = startDate,
            endDate = endDate,
            token = "Bearer $token"
        )
        return if (response.isSuccessful) {
            response.body() ?: emptyList()
        } else emptyList()
    }



    val createDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isExporting = true
                try {
                    PDFBoxResourceLoader.init(context)
                    val selectedAnimalDetails = animals.filter { it.id in selectedAnimals }
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        generateAnimalPDF(outputStream, selectedAnimalDetails, folderName)
                    }
                    exportMessage = "PDF exported successfully!"
                } catch (e: Exception) {
                    exportMessage = "Error exporting PDF: ${e.message}"
                } finally {
                    isExporting = false
                }
            }
        } ?: run {
            exportMessage = "PDF export cancelled."
        }
    }

    LaunchedEffect(folderId) {
        try {
            isLoading = true
            errorMessage = null
            
            // Fetch folder details
            val folderResponse = RetrofitClient.apiService.getFolder(folderId, "Bearer $token")
            if (folderResponse.isSuccessful) {
                folderName = folderResponse.body()?.name ?: "Unknown Folder"
            } else {
                errorMessage = "Failed to load folder details: ${folderResponse.code()}"
                folderName = "Unknown Folder"
            }

            // Fetch animals
            val animalsResponse = RetrofitClient.apiService.getAnimalsByFolderId(folderId, "Bearer $token")
            if (animalsResponse.isSuccessful) {
                animals = animalsResponse.body() ?: emptyList()
                filteredAnimals = animals
            } else {
                errorMessage = "Failed to load animals: ${animalsResponse.code()}"
                animals = emptyList()
                filteredAnimals = emptyList()
            }
        } catch (e: Exception) {
            errorMessage = "Error: ${e.message}"
            folderName = "Unknown Folder"
            animals = emptyList()
            filteredAnimals = emptyList()
        } finally {
            isLoading = false
        }
    }

    // Search functionality
    LaunchedEffect(addByIdInput) {
        if (addByIdInput.isBlank()) {
            matchingAnimals = emptyList()
            return@LaunchedEffect
        }

        try {
            val response = RetrofitClient.apiService.searchAnimals(addByIdInput, "Bearer $token")
            if (response.isSuccessful) {
                matchingAnimals = response.body() ?: emptyList()
            } else {
                Timber.tag("SearchAnimals").e("Search failed: ${response.code()}")
                matchingAnimals = emptyList()
            }
        } catch (e: Exception) {
            Timber.tag("SearchAnimals").e("Error searching animals: ${e.message}")
            matchingAnimals = emptyList()
        }
    }

    LaunchedEffect(searchQuery, sortOrder) {
        filteredAnimals = if (searchQuery.isEmpty()) {
            animals
        } else {
            animals.filter { animal ->
                animal.id.contains(searchQuery, ignoreCase = true) ||
                        animal.birthDate.contains(searchQuery, ignoreCase = true)
            }
        }

        filteredAnimals = when (sortOrder) {
            SortOrder.BIRTHDATE_ASC -> filteredAnimals.sortedBy { it.birthDate }
            SortOrder.BIRTHDATE_DESC -> filteredAnimals.sortedByDescending { it.birthDate }
            SortOrder.ID_ASC -> filteredAnimals.sortedBy { it.id }
            SortOrder.ID_DESC -> filteredAnimals.sortedByDescending { it.id }
            SortOrder.NONE -> filteredAnimals
        }
    }

    // Functie pentru a verifica daca folderul este generat dupa un eveniment
    fun isGeneratedEventFolder(name: String): Boolean {
        return name.startsWith("Births") || name.startsWith("Sickness:") || name.startsWith("Vaccination:")
    }

    // Tabs pentru filtre
    val baseTabs = listOf("Sex", "Birth Date", "Species", "Milk Producers")
    val eventTab = if (isGeneratedEventFolder(folderName)) listOf("Event Period") else emptyList()
    val tabTitles = baseTabs + eventTab

    // State pentru filtrul de perioada eveniment
    var eventFilterStartDate by remember { mutableStateOf("") }
    var eventFilterEndDate by remember { mutableStateOf("") }
    var eventDateRange by remember { mutableStateOf(0f..1f) }
    // Calculeaza range-ul de date pentru evenimente
    val eventDateRangeBounds = remember(animals) {
        val dates = animals.mapNotNull {
            try { LocalDate.parse(it.birthDate) } catch (e: Exception) { null }
        }
        if (dates.isEmpty()) Pair(LocalDate.now(), LocalDate.now())
        else Pair(dates.minOrNull()!!, dates.maxOrNull()!!)
    }
    fun sliderValueToEventDate(value: Float): LocalDate {
        val daysBetween = ChronoUnit.DAYS.between(eventDateRangeBounds.first, eventDateRangeBounds.second)
        val daysToAdd = (daysBetween * value).toLong()
        return eventDateRangeBounds.first.plusDays(daysToAdd)
    }
    fun eventDateToSliderValue(date: LocalDate): Float {
        val daysBetween = ChronoUnit.DAYS.between(eventDateRangeBounds.first, eventDateRangeBounds.second)
        val daysFromStart = ChronoUnit.DAYS.between(eventDateRangeBounds.first, date)
        return if (daysBetween > 0) daysFromStart.toFloat() / daysBetween else 0f
    }
    fun isValidEventDateString(dateStr: String): Boolean {
        return try { LocalDate.parse(dateStr); true } catch (e: Exception) { false }
    }
    fun eventDateStringToSliderValue(dateStr: String): Float {
        return try { eventDateToSliderValue(LocalDate.parse(dateStr)) } catch (e: Exception) { 0f }
    }
    fun updateEventDateRange(startDate: String, endDate: String) {
        if (isValidEventDateString(startDate) && isValidEventDateString(endDate)) {
            val start = eventDateStringToSliderValue(startDate)
            val end = eventDateStringToSliderValue(endDate)
            if (start <= end) {
                eventDateRange = start..end
            }
        }
    }
    LaunchedEffect(eventDateRange) {
        eventFilterStartDate = sliderValueToEventDate(eventDateRange.start).format(DateTimeFormatter.ISO_DATE)
        eventFilterEndDate = sliderValueToEventDate(eventDateRange.endInclusive).format(DateTimeFormatter.ISO_DATE)
    }
    fun applyFilters() {
        // Filtrare standard
        var result = animals.filter { animal ->
            val sexOk = filterSex == null || animal.gender.equals(filterSex, ignoreCase = true)
            val birthDateOk = (filterBirthDateFrom.isBlank() || animal.birthDate >= filterBirthDateFrom) &&
                    (filterBirthDateTo.isBlank() || animal.birthDate <= filterBirthDateTo)
            val speciesOk = filterSpecies.isEmpty() || filterSpecies.contains(animal.species)
            val milkOk = when {
                filterMilkYes && !filterMilkNo -> animal.producesMilk == true
                !filterMilkYes && filterMilkNo -> animal.producesMilk == false
                filterMilkYes && filterMilkNo -> true
                else -> true
            }
            sexOk && birthDateOk && speciesOk && milkOk
        }
        if (tabTitles[selectedTab] == "Event Period" && isGeneratedEventFolder(folderName)) {
            val eventType = when {
                folderName.startsWith("Births") -> "birth"
                folderName.startsWith("Sickness:") -> "sickness"
                folderName.startsWith("Vaccination:") -> "vaccination"
                else -> ""
            }
            scope.launch {
                filteredAnimals = filterAnimalsByEventPeriod(eventType, eventFilterStartDate, eventFilterEndDate, token)
            }
        } else {
            filteredAnimals = result
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
                        Text("Animals (" + filteredAnimals.size + ")")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        folderCameraViewModel.startNewSession()
                        navController.navigate("folderCamera/$token/$folderId?existingAnimalIds=${animals.map { it.id }.joinToString(",")}")
                    }) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Open Camera")
                    }
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Show Filters")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(MaterialTheme.colorScheme.primary)
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (errorMessage != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                    Button(
                        onClick = {
                            // Retry loading
                            scope.launch {
                                try {
                                    isLoading = true
                                    errorMessage = null
                                    val folderResponse = RetrofitClient.apiService.getFolder(folderId, "Bearer $token")
                                    if (folderResponse.isSuccessful) {
                                        folderName = folderResponse.body()?.name ?: "Unknown Folder"
                                    }
                                    val animalsResponse = RetrofitClient.apiService.getAnimalsByFolderId(folderId, "Bearer $token")
                                    if (animalsResponse.isSuccessful) {
                                        animals = animalsResponse.body() ?: emptyList()
                                        filteredAnimals = animals
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Error: ${e.message}"
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    ) {
                        Text("Retry")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Search Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search animals...") },
                        leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = "Search") },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
                    )
                    var expanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Sort")
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sort by Birthdate (Oldest First)") },
                            onClick = {
                                sortOrder = SortOrder.BIRTHDATE_ASC
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sort by Birthdate (Newest First)") },
                            onClick = {
                                sortOrder = SortOrder.BIRTHDATE_DESC
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sort by ID (Ascending)") },
                            onClick = {
                                sortOrder = SortOrder.ID_ASC
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sort by ID (Descending)") },
                            onClick = {
                                sortOrder = SortOrder.ID_DESC
                                expanded = false
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Remove from Folder Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Add Event Button
                    Button(
                        onClick = { showAddEventDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        enabled = selectedAnimals.isNotEmpty() && !isAddingEvents
                    ) {
                        Icon(Icons.Default.Event, contentDescription = "Add Event")
                        Spacer(Modifier.width(4.dp))
                        Text("Add Event")
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    val response = RetrofitClient.apiService.removeAnimalsFromFolder(
                                        folderId = folderId,
                                        animalIds = selectedAnimals.toList(),
                                        token = "Bearer $token"
                                    )
                                    if (response.isSuccessful) {
                                        popupMessage = "Selected animals removed from folder."
                                        selectedAnimals = emptySet()
                                        fetchAnimalsInFolder(folderId, token) {
                                            animals = it
                                            filteredAnimals = it
                                        }
                                    } else {
                                        popupMessage = "Failed to remove animals: ${response.message()}"
                                    }
                                } catch (e: Exception) {
                                    popupMessage = "Error removing animals: ${e.message}"
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        enabled = selectedAnimals.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove from Folder")
                        Spacer(Modifier.width(4.dp))
                        Text("Remove")
                    }
                }

                // Select All Checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Checkbox(
                            checked = selectedAnimals.size == filteredAnimals.size && filteredAnimals.isNotEmpty(),
                            onCheckedChange = { checked ->
                                selectedAnimals = if (checked) {
                                    filteredAnimals.map { it.id }.toSet()
                                } else {
                                    emptySet()
                                }
                            }
                        )
                        Text(
                            text = "Select All",
                            modifier = Modifier.clickable {
                                selectedAnimals = if (selectedAnimals.size == filteredAnimals.size && filteredAnimals.isNotEmpty()) {
                                    emptySet()
                                } else {
                                    filteredAnimals.map { it.id }.toSet()
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Animal List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredAnimals) { animal ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedAnimals = if (selectedAnimals.contains(animal.id)) {
                                        selectedAnimals - animal.id
                                    } else {
                                        selectedAnimals + animal.id
                                    }
                                },
                            elevation = CardDefaults.elevatedCardElevation(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedAnimals.contains(animal.id)) {
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
                                val genderSymbol = if (animal.gender.equals("m", ignoreCase = true)) "♂" else "♀"
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${animal.id} $genderSymbol", style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        text = "Born: ${animal.birthDate}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                // Navigation to Animal Details
                                IconButton(onClick = {
                                    navController.navigate("animalDetails/${animal.id}/{token}")
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Details"
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Export Button
                    Button(
                        onClick = {
                            if (selectedAnimals.isEmpty()) {
                                exportMessage = "Please select animals to export"
                                return@Button
                            }
                            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                            val filename = "${folderName}_animal_list_$timestamp.pdf"
                            createDocumentLauncher.launch(filename)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isExporting
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Export to PDF",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isExporting) "Exporting..." else "Export to PDF")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Add by ID Button
                    Button(
                        onClick = { showAddByIdDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Add by ID")
                    }
                }
            }
        }
    }

    // Add by ID Dialog
    if (showAddByIdDialog) {
        AlertDialog(
            onDismissRequest = { 
                showAddByIdDialog = false
                addByIdInput = ""
                matchingAnimals = emptyList()
            },
            title = { Text("Add Animal by ID") },
            text = {
                Column {
                    OutlinedTextField(
                        value = addByIdInput,
                        onValueChange = { addByIdInput = it },
                        label = { Text("Enter Animal ID") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (matchingAnimals.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Matching Animals:", style = MaterialTheme.typography.titleSmall)
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 200.dp)
                        ) {
                            items(matchingAnimals) { animal ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            scope.launch {
                                                try {
                                                    if (animals.any { it.id == animal.id }) {
                                                        popupMessage = "Animal ${animal.id} is already in this folder."
                                                        showAddByIdDialog = false
                                                        addByIdInput = ""
                                                        matchingAnimals = emptyList()
                                                        return@launch 
                                                    }
                                                    val response = RetrofitClient.apiService.addAnimalToFolder(
                                                        folderId = folderId,
                                                        animalId = animal.id,
                                                        token= "Bearer $token"
                                                    )
                                                    if (response.isSuccessful) {
                                                        fetchAnimalsInFolder(folderId, token) {
                                                            animals = it
                                                            filteredAnimals = it
                                                        }
                                                        showAddByIdDialog = false
                                                        addByIdInput = ""
                                                        matchingAnimals = emptyList()
                                                    }
                                                } catch (e: Exception) {
                                                    Log.e("AddAnimal", "Error adding animal: ${e.message}")
                                                }
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(animal.id, style = MaterialTheme.typography.bodyMedium)
                                            Text(
                                                "Born: ${animal.birthDate}",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Add to folder",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { 
                        showAddByIdDialog = false
                        addByIdInput = ""
                        matchingAnimals = emptyList()
                    }
                ) {
                    Text("Close")
                }
            }
        )
    }

    // Add Event Dialog
    if (showAddEventDialog) {
        AddEventDialog(
            onDismiss = { showAddEventDialog = false },
            onSave = { eventType, details ->
                showAddEventDialog = false
                scope.launch {
                    isAddingEvents = true
                    var successCount = 0
                    var failureCount = 0
                    selectedAnimals.forEach { animalId ->
                        try {
                            val response = RetrofitClient.apiService.postEvent(animalId, "Bearer $token", details)
                            if (response.isSuccessful) {
                                successCount++
                            } else {
                                failureCount++
                            }
                        } catch (e: Exception) {
                            failureCount++
                        }
                    }
                    popupMessage = when {
                        successCount > 0 && failureCount == 0 -> "Successfully added event to all selected animals"
                        successCount > 0 && failureCount > 0 -> "Added event to $successCount animals, failed for $failureCount animals"
                        else -> "Failed to add event to any animals"
                    }
                    isAddingEvents = false
                }
            }
        )
    }

    // Export Message Dialog
    popupMessage?.let {
        AlertDialog(
            onDismissRequest = { popupMessage = null },
            title = { Text("Status") },
            text = { Text(it) },
            confirmButton = {
                Button(onClick = { popupMessage = null }) {
                    Text("OK")
                }
            }
        )
    }

    // ALERT DIALOG FILTRE
    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("Filters") },
            text = {
                Column {
                    val baseTabs = listOf("Sex", "Birth Date", "Species", "Milk Producers")
                    val eventTab = if (isGeneratedEventFolder(folderName)) listOf("Event Period") else emptyList()
                    val tabTitles = baseTabs + eventTab
                    ScrollableTabRow(selectedTabIndex = selectedTab) {
                        tabTitles.forEachIndexed { idx, title ->
                            Tab(
                                selected = selectedTab == idx,
                                onClick = { selectedTab = idx },
                                text = { Text(title) }
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    when (tabTitles[selectedTab]) {
                        "Sex" -> { // Sex
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = filterSex == "F",
                                    onCheckedChange = { checked -> filterSex = if (checked) "F" else null }
                                )
                                Text("Female", Modifier.clickable { filterSex = if (filterSex == "F") null else "F" })
                                Spacer(Modifier.width(16.dp))
                                Checkbox(
                                    checked = filterSex == "M",
                                    onCheckedChange = { checked -> filterSex = if (checked) "M" else null }
                                )
                                Text("Male", Modifier.clickable { filterSex = if (filterSex == "M") null else "M" })
                            }
                        }
                        "Birth Date" -> { // Birth Date
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Text(
                                    text = "Birth Date Range",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    OutlinedTextField(
                                        value = filterBirthDateFrom,
                                        onValueChange = { newValue ->
                                            filterBirthDateFrom = newValue
                                            if (isValidDateString(newValue)) {
                                                updateBirthDateRange(newValue, filterBirthDateTo.ifEmpty { dateRange.second.format(DateTimeFormatter.ISO_DATE) })
                                            }
                                        },
                                        placeholder = {
                                            Text(dateRange.first.format(DateTimeFormatter.ISO_DATE))
                                        },
                                        modifier = Modifier.weight(1f),
                                        textStyle = MaterialTheme.typography.bodyMedium,
                                        singleLine = true
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    OutlinedTextField(
                                        value = filterBirthDateTo,
                                        onValueChange = { newValue ->
                                            filterBirthDateTo = newValue
                                            if (isValidDateString(newValue)) {
                                                updateBirthDateRange(filterBirthDateFrom.ifEmpty { dateRange.first.format(DateTimeFormatter.ISO_DATE) }, newValue)
                                            }
                                        },
                                        placeholder = {
                                            Text(dateRange.second.format(DateTimeFormatter.ISO_DATE))
                                        },
                                        modifier = Modifier.weight(1f),
                                        textStyle = MaterialTheme.typography.bodyMedium,
                                        singleLine = true
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                RangeSlider(
                                    value = birthDateRange,
                                    onValueChange = { birthDateRange = it },
                                    valueRange = 0f..1f,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        "Species" -> { // Species
                            Column {
                                allSpecies.forEach { species ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = filterSpecies.contains(species),
                                            onCheckedChange = { checked ->
                                                filterSpecies = if (checked) filterSpecies + species else filterSpecies - species
                                            }
                                        )
                                        Text(species, Modifier.clickable {
                                            filterSpecies = if (filterSpecies.contains(species)) filterSpecies - species else filterSpecies + species
                                        })
                                    }
                                }
                            }
                        }
                        "Milk Producers" -> { // Milk Producers
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = filterMilkYes,
                                    onCheckedChange = { checked -> filterMilkYes = checked }
                                )
                                Text("Produce lapte", Modifier.clickable { filterMilkYes = !filterMilkYes })
                                Spacer(Modifier.width(16.dp))
                                Checkbox(
                                    checked = filterMilkNo,
                                    onCheckedChange = { checked -> filterMilkNo = checked }
                                )
                                Text("Nu produce lapte", Modifier.clickable { filterMilkNo = !filterMilkNo })
                            }
                        }
                        "Event Period" -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Text(
                                    text = "Event Period Range",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    OutlinedTextField(
                                        value = eventFilterStartDate,
                                        onValueChange = { newValue ->
                                            eventFilterStartDate = newValue
                                            if (isValidEventDateString(newValue)) {
                                                updateEventDateRange(newValue, eventFilterEndDate.ifEmpty { eventDateRangeBounds.second.format(DateTimeFormatter.ISO_DATE) })
                                            }
                                        },
                                        placeholder = { Text(eventDateRangeBounds.first.format(DateTimeFormatter.ISO_DATE)) },
                                        modifier = Modifier.weight(1f),
                                        textStyle = MaterialTheme.typography.bodyMedium,
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    OutlinedTextField(
                                        value = eventFilterEndDate,
                                        onValueChange = { newValue ->
                                            eventFilterEndDate = newValue
                                            if (isValidEventDateString(newValue)) {
                                                updateEventDateRange(eventFilterStartDate.ifEmpty { eventDateRangeBounds.first.format(DateTimeFormatter.ISO_DATE) }, newValue)
                                            }
                                        },
                                        placeholder = { Text(eventDateRangeBounds.second.format(DateTimeFormatter.ISO_DATE)) },
                                        modifier = Modifier.weight(1f),
                                        textStyle = MaterialTheme.typography.bodyMedium,
                                        singleLine = true
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                RangeSlider(
                                    value = eventDateRange,
                                    onValueChange = { eventDateRange = it },
                                    valueRange = 0f..1f,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row {
                    Button(onClick = {
                        applyFilters()
                        showFilterDialog = false
                    }) { Text("Apply Filters") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {
                        filterSex = null
                        filterBirthDateFrom = ""
                        filterBirthDateTo = ""
                        filterSpecies = emptySet()
                        filterMilkYes = false
                        filterMilkNo = false
                        filteredAnimals = animals
                        eventFilterStartDate = ""
                        eventFilterEndDate = ""
                        eventDateRange = 0f..1f
                        showFilterDialog = false
                    }) { Text("Reset") }
                }
            }
        )
    }
}

fun fetchAnimalsInFolder(folderId: String, token: String, onResult: (List<AnimalDetails>) -> Unit) {
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




private suspend fun generateAnimalPDF(outputStream: OutputStream, animals: List<AnimalDetails>, folderName: String) = withContext(Dispatchers.IO) {
    val document = PDDocument()
    var page = PDPage()
    document.addPage(page)
    var contentStream = PDPageContentStream(document, page)
    val margin = 50f
    var currentY = page.mediaBox.height - margin

    contentStream.beginText()
    contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14f)
    contentStream.setTextMatrix(Matrix.getTranslateInstance(margin, currentY))
    contentStream.showText("Folder: $folderName")
    contentStream.endText()

    currentY -= 20f

    contentStream.beginText()
    contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14f)
    contentStream.setTextMatrix(Matrix.getTranslateInstance(margin, currentY))
    contentStream.showText("Formular de identificare")
    contentStream.endText()


    val tableWidth = PDPage().mediaBox.width - 2 * margin
    val cellMargin = 5f
    val rowHeight = 20f
    val headerHeight = 25f
    val tableStartYOffset = 30f
    val tableBottomY = margin

    val columnWidths = floatArrayOf(30f, 80f, 80f, 40f, 100f, tableWidth - 330f)
    val headers = listOf("Nr.", "Specie", "Rasa", "Sex", "Data nastere", "Cod identificare")

    fun String.replaceRomanianChars(): String {
        return this
            .replace('ț', 't')
            .replace('ș', 's')
            .replace('ă', 'a')
            .replace('î', 'i')
            .replace('â', 'a')
            .replace('Ț', 'T')
            .replace('Ș', 'S')
            .replace('Ă', 'A')
            .replace('Î', 'I')
            .replace('Â', 'A')
    }

    fun drawHeader(contentStream: PDPageContentStream, startY: Float) {
        var currentX = margin
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 10f)
        for (i in headers.indices) {
            contentStream.beginText()
            contentStream.setTextMatrix(Matrix.getTranslateInstance(currentX + cellMargin, startY - headerHeight + (headerHeight - 10f) / 2f))
            contentStream.showText(headers[i].replaceRomanianChars())
            contentStream.endText()
            currentX += columnWidths[i]
        }

        contentStream.setLineWidth(0.5f)
        contentStream.moveTo(margin, startY - headerHeight)
        contentStream.lineTo(margin + tableWidth, startY - headerHeight)
        contentStream.stroke()

        currentX = margin
        for (width in columnWidths) {
            contentStream.moveTo(currentX, startY)
            contentStream.lineTo(currentX, startY - headerHeight)
            contentStream.stroke()
            currentX += width
        }
        contentStream.moveTo(margin + tableWidth, startY)
        contentStream.lineTo(margin + tableWidth, startY - headerHeight)
        contentStream.stroke()
    }



    contentStream.beginText()
    contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14f)
    contentStream.setTextMatrix(Matrix.getTranslateInstance(margin, currentY))
    contentStream.showText("Formular de identificare".replaceRomanianChars())
    contentStream.endText()

    currentY -= 20f

    contentStream.beginText()
    contentStream.setFont(PDType1Font.HELVETICA, 10f)
    contentStream.setTextMatrix(Matrix.getTranslateInstance(margin, currentY))
    contentStream.showText("Animal List Export".replaceRomanianChars())
    contentStream.endText()

    currentY -= tableStartYOffset
    val tableStartX = margin

    drawHeader(contentStream, currentY)
    currentY -= headerHeight

    contentStream.setFont(PDType1Font.HELVETICA, 8f)

    // Table content
    animals.forEachIndexed { index, animal ->
        // Check if new page is needed
        if (currentY - rowHeight < tableBottomY) { // Check if enough space for next row
            contentStream.close()
            page = PDPage()
            document.addPage(page)
            contentStream = PDPageContentStream(document, page)
            currentY = page.mediaBox.height - margin // Reset Y for new page
            drawHeader(contentStream, currentY)
            currentY -= headerHeight // Move below header on new page
            contentStream.setFont(PDType1Font.HELVETICA, 8f) // Use standard font for table content on new page
        }

        // Draw row content
        var currentColumnX = tableStartX
        val rowY = currentY - (rowHeight / 2f) + (8f / 2f)

        // Nr.
        contentStream.beginText()
        contentStream.setTextMatrix(Matrix.getTranslateInstance(currentColumnX + cellMargin, rowY))
        contentStream.showText((index + 1).toString())
        contentStream.endText()
        currentColumnX += columnWidths[0]

        // Specie
        contentStream.beginText()
        contentStream.setTextMatrix(Matrix.getTranslateInstance(currentColumnX + cellMargin, rowY))
        contentStream.showText("Ovine".replaceRomanianChars())
        contentStream.endText()
        currentColumnX += columnWidths[1]

        // Rasa
        contentStream.beginText()
        contentStream.setTextMatrix(Matrix.getTranslateInstance(currentColumnX + cellMargin, rowY))
        contentStream.showText(animal.species.replaceRomanianChars())
        contentStream.endText()
        currentColumnX += columnWidths[2]

        // Sex
        contentStream.beginText()
        contentStream.setTextMatrix(Matrix.getTranslateInstance(currentColumnX + cellMargin, rowY))
        val genderText = when (animal.gender.toLowerCase()) {
            "male", "m", "F", "♂" -> "male"
            "female", "f","M", "♀" -> "female"
            else -> animal.gender.replaceRomanianChars()
        }
        contentStream.showText(genderText)
        contentStream.endText()
        currentColumnX += columnWidths[3]

        // Data nastere
        contentStream.beginText()
        contentStream.setTextMatrix(Matrix.getTranslateInstance(currentColumnX + cellMargin, rowY))
        contentStream.showText(animal.birthDate.replaceRomanianChars())
        contentStream.endText()
        currentColumnX += columnWidths[4]

        // Cod identificare
        contentStream.beginText()
        contentStream.setTextMatrix(Matrix.getTranslateInstance(currentColumnX + cellMargin, rowY))
        contentStream.showText(animal.id.replaceRomanianChars())
        contentStream.endText()
        currentColumnX += columnWidths[5]

        contentStream.setLineWidth(0.5f)
        contentStream.moveTo(margin, currentY - rowHeight)
        contentStream.lineTo(margin + tableWidth, currentY - rowHeight)
        contentStream.stroke()

        currentColumnX = margin
        for (width in columnWidths) {
            contentStream.moveTo(currentColumnX, currentY)
            contentStream.lineTo(currentColumnX, currentY - rowHeight)
            contentStream.stroke()
            currentColumnX += width
        }
        contentStream.moveTo(margin + tableWidth, currentY)
        contentStream.lineTo(margin + tableWidth, currentY - rowHeight)
        contentStream.stroke()

        currentY -= rowHeight
    }


    contentStream.close()
    document.save(outputStream)
    document.close()
}