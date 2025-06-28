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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import android.content.Context
import android.os.Environment
import androidx.compose.ui.platform.LocalContext
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.font.PDType0Font
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import com.tom_roush.pdfbox.util.Matrix
import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.Image
import java.io.OutputStream
import androidx.compose.material3.TabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.RangeSlider
import java.time.temporal.ChronoUnit
import androidx.compose.ui.res.painterResource

@SuppressLint("NewApi")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimalListScreen(token: String, navController: NavController, cameraViewModel: CameraViewModel) {
    val scope = rememberCoroutineScope()
    var animals by remember { mutableStateOf<List<AnimalDetails>>(emptyList()) }
    var filteredAnimals by remember { mutableStateOf<List<AnimalDetails>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedAnimals by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isCounting by remember { mutableStateOf(false) }
    var sortOrder by remember { mutableStateOf<SortOrder>(SortOrder.NONE) }
    var isExporting by remember { mutableStateOf(false) }
    var exportMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    
    var showFolderDialog by remember { mutableStateOf(false) }
    var folders by remember { mutableStateOf<List<Folder>>(emptyList()) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var selectedFolderId by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                        generateAnimalPDF(outputStream, selectedAnimalDetails)
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

    LaunchedEffect(Unit) {
        loadAnimals(token) { result ->
            animals = result
            filteredAnimals = result
        }
    }

    LaunchedEffect(navController.currentBackStackEntry) {
        val currentEntry = navController.currentBackStackEntry
        currentEntry?.savedStateHandle?.getLiveData<Boolean>("refresh")?.observeForever {
            if (it == true) {
                loadAnimals(token) { result ->
                    animals = result
                    filteredAnimals = result
                }
                currentEntry.savedStateHandle.set("refresh", false)
            }
        }
    }

    LaunchedEffect(searchQuery, sortOrder) {
        filteredAnimals = if (searchQuery.isEmpty()) {
            animals
        } else {
            animals.filter {
                it.id.contains(searchQuery, ignoreCase = true) ||
                        it.birthDate.contains(searchQuery, ignoreCase = true)
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

    LaunchedEffect(showFolderDialog) {
        if (showFolderDialog) {
            try {
                val response = RetrofitClient.apiService.getFolders("Bearer $token")
                if (response.isSuccessful) {
                    folders = response.body() ?: emptyList()
                } else {
                    errorMessage = "Failed to load folders"
                }
            } catch (e: Exception) {
                errorMessage = "Error loading folders: ${e.message}"
            }
        }
    }

    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    var filterSex by remember { mutableStateOf<String?>(null) }
    var filterBirthDateFrom by remember { mutableStateOf("") }
    var filterBirthDateTo by remember { mutableStateOf("") }
    var birthDateRange by remember { mutableStateOf(0f..1f) }
    var filterSpecies by remember { mutableStateOf<Set<String>>(emptySet()) }
    var filterMilkYes by remember { mutableStateOf(false) }
    var filterMilkNo by remember { mutableStateOf(false) }

    // range animale
    val dateRange = remember(animals) {
        if (animals.isEmpty()) {
            Pair(LocalDate.now(), LocalDate.now())
        } else {
            val dates = animals.map { LocalDate.parse(it.birthDate) }
            Pair(dates.minOrNull() ?: LocalDate.now(), LocalDate.now())
        }
    }

    // Convertire date to value
    @SuppressLint("NewApi")
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

    // Validare date string format
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

    // Extrage toate speciile distincte
    val allSpecies = animals.map { it.species }.distinct().sorted()


    fun applyFilters() {
        filteredAnimals = animals.filter { animal ->
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
                        cameraViewModel.startNewSession()
                        navController.navigate("camera/$token/${animals.map{it.id}.joinToString(",")}")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
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
                        })
                    Text(
                        text = "Select All",
                        modifier = Modifier.clickable {
                            selectedAnimals = if (selectedAnimals.size == filteredAnimals.size) {
                                emptySet()
                            } else {
                                filteredAnimals.map { it.id }.toSet()
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { showFolderDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = "Add to Folder",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add to Folder")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                            IconButton(onClick = {
                                navController.previousBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("refresh", true)
                                navController.navigate("animalDetails/${animal.id}/${token}")
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
                Button(
                    onClick = {
                        if (selectedAnimals.isEmpty()) {
                            exportMessage = "Please select animals to export"
                            return@Button
                        }
                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                        val filename = "animal_list_$timestamp.pdf"
                        createDocumentLauncher.launch(filename)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
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

                Button(
                    onClick = { navController.navigate("fileUpload/$token") },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Add Animals")
                }
            }
        }
    }

    // Folder Selection Dialog
    if (showFolderDialog) {
        AlertDialog(
            onDismissRequest = { 
                showFolderDialog = false
                selectedFolderId = null
                errorMessage = null
            },
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
                        onClick = { showFolderDialog = false }
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
                                            animalIds = selectedAnimals.toList(),
                                            authorization = "Bearer $token"
                                        )
                                        if (response.isSuccessful) {
                                            showFolderDialog = false
                                            selectedFolderId = null
                                            selectedAnimals = emptySet()
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
    }

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

    // ALERT DIALOG FILTRE
    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("Filters") },
            text = {
                Column {
                    val tabTitles = listOf("Sex", "Birth Date", "Species", "Milk Producers")
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
                    when (selectedTab) {
                        0 -> {
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
                        1 -> { // Birth Date
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
                        2 -> { // Species
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
                        3 -> { // Milk Producers
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = filterMilkYes,
                                    onCheckedChange = { checked -> filterMilkYes = checked }
                                )
                                Text("YES", Modifier.clickable { filterMilkYes = !filterMilkYes })
                                Spacer(Modifier.width(16.dp))
                                Checkbox(
                                    checked = filterMilkNo,
                                    onCheckedChange = { checked -> filterMilkNo = checked }
                                )
                                Text("NO", Modifier.clickable { filterMilkNo = !filterMilkNo })
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
                        showFilterDialog = false
                    }) { Text("Reset") }
                }
            }
        )
    }

    exportMessage?.let {
        AlertDialog(
            onDismissRequest = { exportMessage = null },
            title = { Text("Export Status") },
            text = { Text(it) },
            confirmButton = {
                Button(onClick = { exportMessage = null }) {
                    Text("OK")
                }
            }
        )
    }


}

fun loadAnimals(token: String,  onComplete: (List<AnimalDetails>) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = RetrofitClient.apiService.getAnimalsByOwnerId( "Bearer $token")
            if (response.isSuccessful) {
                onComplete(response.body() ?: emptyList())
            } else {
                onComplete(emptyList())
                Log.e("LoadAnimals", "Error fetching animals: ${response.message()}")
            }
        } catch (e: Exception) {
            onComplete(emptyList())
            Log.e("LoadAnimals", "Error: ${e.message}")
        }
    }
}

private suspend fun generateAnimalPDF(outputStream: OutputStream, animals: List<AnimalDetails>) = withContext(Dispatchers.IO) {
    val document = PDDocument()

    val margin = 50f
    val tableWidth = PDPage().mediaBox.width - 2 * margin
    val cellMargin = 5f
    val rowHeight = 20f
    val headerHeight = 25f
    val tableStartYOffset = 30f
    val tableBottomY = margin

    val columnWidths = floatArrayOf(30f, 80f, 80f, 40f, 100f, tableWidth - 330f)

    // Function to replace Romanian characters
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
    val headers = listOf("Nr.", "Specie", "Rasa", "Sex", "Data nastere", "Cod identificare")
    fun drawHeader(contentStream: PDPageContentStream, startY: Float) {
        var currentX = margin
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 10f)
        for (i in headers.indices) {
            contentStream.beginText()
            contentStream.setTextMatrix(Matrix.getTranslateInstance(currentX + cellMargin,
                startY - headerHeight + (headerHeight - 10f) / 2f))
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

    var page = PDPage()
    document.addPage(page)
    var contentStream = PDPageContentStream(document, page)
    var currentY = page.mediaBox.height - margin

    // Draw initial header
    contentStream.beginText()
    contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14f)
    contentStream.setTextMatrix(Matrix.getTranslateInstance(margin, currentY))
    contentStream.showText("Formular de identificare".replaceRomanianChars())
    contentStream.endText()

    currentY -= 20f

    contentStream.beginText()
    contentStream.setFont(PDType1Font.HELVETICA, 10f)
    contentStream.setTextMatrix(Matrix.getTranslateInstance(margin, currentY))
    contentStream.showText("Animal List Export ".replaceRomanianChars())
    contentStream.endText()

    currentY -= tableStartYOffset
    val tableStartX = margin

    drawHeader(contentStream, currentY)
    currentY -= headerHeight

    contentStream.setFont(PDType1Font.HELVETICA, 8f)

    // Table content
    animals.forEachIndexed { index, animal ->
        if (currentY - rowHeight < tableBottomY) {
            contentStream.close()
            page = PDPage()
            document.addPage(page)
            contentStream = PDPageContentStream(document, page)
            currentY = page.mediaBox.height - margin
            drawHeader(contentStream, currentY)
            currentY -= headerHeight
            contentStream.setFont(PDType1Font.HELVETICA, 8f)
        }

        var currentColumnX = tableStartX
        val rowY = currentY - (rowHeight / 2f) + (8f / 2f)

        contentStream.beginText()
        contentStream.setTextMatrix(Matrix.getTranslateInstance(currentColumnX + cellMargin, rowY))
        contentStream.showText((index + 1).toString())
        contentStream.endText()
        currentColumnX += columnWidths[0]

        contentStream.beginText()
        contentStream.setTextMatrix(Matrix.getTranslateInstance(currentColumnX + cellMargin, rowY))
        contentStream.showText("Ovine".replaceRomanianChars())
        contentStream.endText()
        currentColumnX += columnWidths[1]

        contentStream.beginText()
        contentStream.setTextMatrix(Matrix.getTranslateInstance(currentColumnX + cellMargin, rowY))
        contentStream.showText(animal.species.replaceRomanianChars())
        contentStream.endText()
        currentColumnX += columnWidths[2]

        contentStream.beginText()
        contentStream.setTextMatrix(Matrix.getTranslateInstance(currentColumnX + cellMargin, rowY))
        val genderText = when (animal.gender.toLowerCase()) {
            "male", "m", "♂" -> "male"
            "female", "f", "♀" -> "female"
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
