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
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.util.Matrix
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.OutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimalListScreenByFolder(
    token: String,
    folderId: String,
    navController: NavController
) {
    var animals by remember { mutableStateOf<List<Animal>>(emptyList()) }
    var filteredAnimals by remember { mutableStateOf<List<Animal>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedAnimals by remember { mutableStateOf<Set<String>>(emptySet()) }
    var sortOrder by remember { mutableStateOf<SortOrder>(SortOrder.NONE) }
    var showAddByIdDialog by remember { mutableStateOf(false) }
    var showAddEventDialog by remember { mutableStateOf(false) }
    var addByIdInput by remember { mutableStateOf("") }
    var matchingAnimals by remember { mutableStateOf<List<Animal>>(emptyList()) }
    var isExporting by remember { mutableStateOf(false) }
    var exportMessage by remember { mutableStateOf<String?>(null) }
    var folderName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var popupMessage by remember { mutableStateOf<String?>(null) }
    var isAddingEvents by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Activity result launcher for creating a PDF file
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

    // Fetch folder name and animals
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

    // Search functionality for Add by ID
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Animals in $folderName") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        navController.navigate("folderCamera/$token/$folderId?existingAnimalIds=${animals.map { it.id }.joinToString(",")}")
                    }) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Open Camera")
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
                // Search Bar with Sort Icon
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

                // Remove from Folder Button (Moved)
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
                                val genderSymbol = if (animal.gender.equals("male", ignoreCase = true)) "♂" else "♀"
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
                                    navController.navigate("animalDetails/${animal.id}")
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

    // Export Message Dialog (also used for event messages)
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
}

fun fetchAnimalsInFolder(folderId: String, token: String, onResult: (List<Animal>) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = RetrofitClient.apiService.getAnimalsByFolderId(folderId, "Bearer $token")
            if (response.isSuccessful) {
                onResult(response.body()?.filterNotNull() ?: emptyList())
            } else {
                onResult(emptyList()) // Handle failure gracefully
            }
        } catch (e: Exception) {
            onResult(emptyList()) // Handle exception gracefully
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
        // Check if the scanned ID exists in the global list
        val globalResponse = RetrofitClient.apiService.checkAnimalExists(scannedId, "Bearer $token")
        if (!globalResponse.isSuccessful || globalResponse.body() != true) {
            onResult("The ID is not present in the global animal list.")
            return
        }

        // Check if the animal is already in the folder
        if (currentAnimals.any { it.id == scannedId }) {
            onResult("This animal is already in the folder.")
            return
        }

        // Add the animal to the folder
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
                onComplete() // Notify the caller on success
            } else {
                Log.e("DeleteAnimals", "Failed to remove animals: ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("DeleteAnimals", "Error removing animals from folder: ${e.message}")
        }
    }
}

// Update the generateAnimalPDF function to include folder name
private suspend fun generateAnimalPDF(outputStream: OutputStream, animals: List<Animal>, folderName: String) = withContext(Dispatchers.IO) {
    val document = PDDocument()
    var page = PDPage()
    document.addPage(page)
    var contentStream = PDPageContentStream(document, page)
    val margin = 50f
    var currentY = page.mediaBox.height - margin

    // Draw header content with folder name
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
    val tableStartYOffset = 30f // Space after header content
    val tableBottomY = margin

    val columnWidths = floatArrayOf(30f, 80f, 80f, 40f, 100f, tableWidth - 330f)
    val headers = listOf("Nr.", "Specie", "Rasa", "Sex", "Data nastere", "Cod identificare")

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

    fun drawHeader(contentStream: PDPageContentStream, startY: Float) {
        var currentX = margin
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 10f)
        for (i in headers.indices) {
            contentStream.beginText()
            contentStream.setTextMatrix(Matrix.getTranslateInstance(currentX + cellMargin, startY - headerHeight + (headerHeight - 10f) / 2f))
            contentStream.showText(headers[i].replaceRomanianChars()) // Replace chars in headers too if needed
            contentStream.endText()
            currentX += columnWidths[i]
        }

        // Draw header lines
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



    // Draw initial header content
    contentStream.beginText()
    contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14f)
    contentStream.setTextMatrix(Matrix.getTranslateInstance(margin, currentY))
    contentStream.showText("Formular de identificare".replaceRomanianChars())
    contentStream.endText()

    currentY -= 20f

    contentStream.beginText()
    contentStream.setFont(PDType1Font.HELVETICA, 10f)
    contentStream.setTextMatrix(Matrix.getTranslateInstance(margin, currentY))
    contentStream.showText("Animal List Export".replaceRomanianChars()) // Placeholder, could add more details if available
    contentStream.endText()

    currentY -= tableStartYOffset // Space before table
    val tableStartX = margin

    drawHeader(contentStream, currentY)
    currentY -= headerHeight // Move below header

    contentStream.setFont(PDType1Font.HELVETICA, 8f) // Use standard font for table content

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
        val rowY = currentY - (rowHeight / 2f) + (8f / 2f) // Center text vertically in row

        // Nr.
        contentStream.beginText()
        contentStream.setTextMatrix(Matrix.getTranslateInstance(currentColumnX + cellMargin, rowY))
        contentStream.showText((index + 1).toString())
        contentStream.endText()
        currentColumnX += columnWidths[0]

        // Specie
        contentStream.beginText()
        contentStream.setTextMatrix(Matrix.getTranslateInstance(currentColumnX + cellMargin, rowY))
        contentStream.showText("Ovine".replaceRomanianChars()) // Using placeholder, need actual specie
        contentStream.endText()
        currentColumnX += columnWidths[1]

        // Rasa
        contentStream.beginText()
        contentStream.setTextMatrix(Matrix.getTranslateInstance(currentColumnX + cellMargin, rowY))
        contentStream.showText(animal.species.replaceRomanianChars()) // Using species as placeholder for breed for now
        contentStream.endText()
        currentColumnX += columnWidths[2]

        // Sex
        contentStream.beginText()
        contentStream.setTextMatrix(Matrix.getTranslateInstance(currentColumnX + cellMargin, rowY))
        val genderText = when (animal.gender.toLowerCase()) {
            "male", "m", "♂" -> "male"
            "female", "f", "♀" -> "female"
            else -> animal.gender.replaceRomanianChars() // Fallback with replacement
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

        // Draw horizontal line below row
        contentStream.setLineWidth(0.5f)
        contentStream.moveTo(margin, currentY - rowHeight)
        contentStream.lineTo(margin + tableWidth, currentY - rowHeight)
        contentStream.stroke()

        // Draw vertical lines for this row
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

        currentY -= rowHeight // Move to the next row position
    }


    contentStream.close()
    document.save(outputStream)
    document.close()
}