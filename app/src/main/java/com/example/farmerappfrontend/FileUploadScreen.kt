package com.example.farmerappfrontend

import TokenManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog

import androidx.compose.material3.*
import androidx.compose.foundation.layout.*

import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAnimalsScreen(token: String, navController: NavController) {
    val customPurple = Color(0xFF6650a4)
    var specie by remember { mutableStateOf("") }
    var animalId by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var producesMilk by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }
    var formSuccess by remember { mutableStateOf<String?>(null) }
    var pdfImportMessage by remember { mutableStateOf<String?>(null) }
    var isImporting by remember { mutableStateOf(false) }
    var pendingAnimals by remember { mutableStateOf<List<AnimalDetails>>(emptyList()) }
    var speciesOptions by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isImporting = true
                try {
                    PDFBoxResourceLoader.init(context)
                    val animals = parsePdfFile(context.contentResolver.openInputStream(uri))
                    pendingAnimals = animals
                } catch (e: Exception) {
                    pdfImportMessage = "Error importing PDF: ${e.message}"
                } finally {
                    isImporting = false
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.apiService.getSpecies()
            if (response.isSuccessful) {
                speciesOptions = response.body()?.map { it["value"]!! to it["label"]!! } ?: emptyList()
            }
        } catch (_: Exception) {}
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HerdID", style = MaterialTheme.typography.headlineMedium, color = Color.White) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = customPurple)
            )
        }
    ) { innerPadding ->
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = { filePickerLauncher.launch("application/pdf") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isImporting
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = "Import PDF", tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text(if (isImporting) "Importing..." else "Import PDF", color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("OR", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))

            pdfImportMessage?.let {
                Text(it, color = if (it.startsWith("Upload")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (pendingAnimals.isNotEmpty()) {
                AlertDialog(
                    onDismissRequest = { pendingAnimals = emptyList() },
                    title = { Text("Confirm Upload") },
                    text = {
                        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                            items(pendingAnimals.size) { index ->
                                val animal = pendingAnimals[index]
                                Text("${index + 1}. ${animal.id} - ${animal.species}")
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            scope.launch {
                                isImporting = true
                                try {
                                    val response = RetrofitClient.apiService.addAnimalsBatch(
                                        "Bearer $token", pendingAnimals
                                    )
                                    if (response.isSuccessful) {
                                        val result = response.body()
                                        val added = (result?.get("added") as? Double)?.toInt() ?: 0
                                        val skipped = (result?.get("skipped") as? Double)?.toInt() ?: 0
                                        pdfImportMessage = "Upload complete: $added added, $skipped skipped."
                                        pendingAnimals = emptyList()
                                    }
                                } catch (e: Exception) {
                                    pdfImportMessage = "Error: ${e.message}"
                                } finally {
                                    isImporting = false
                                }
                            }
                        }) {
                            Text("Confirm")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { pendingAnimals = emptyList() }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(6.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Add Animal Manually", style = MaterialTheme.typography.titleMedium)

                    var expanded by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = speciesOptions.find { it.first == specie }?.second ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Species") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.exposedDropdownSize()
                        ) {
                            speciesOptions.forEach { (value, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        specie = value
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(value = animalId, onValueChange = { animalId = it }, label = { Text("ID") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = birthDate, onValueChange = { birthDate = it }, label = { Text("Birth Date") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = gender, onValueChange = { gender = it }, label = { Text("Gender (M/F)") }, modifier = Modifier.fillMaxWidth())

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Produces Milk", modifier = Modifier.weight(1f))
                        Switch(checked = producesMilk, onCheckedChange = { producesMilk = it })
                    }

                    Button(
                        onClick = {
                            if (specie.isBlank() || animalId.isBlank() || birthDate.isBlank() || gender.isBlank()) {
                                formError = "All fields are required."
                                formSuccess = null
                            } else {
                                scope.launch {
                                    try {
                                        val animal = listOf(AnimalDetails(animalId, gender, birthDate, specie, producesMilk))
                                        val token=TokenManager.getToken(context)
                                        val response = RetrofitClient.apiService.addAnimalsBatch("Bearer $token", animal)
                                        val result = response.body()
                                        val added = (result?.get("added") as? Double)?.toInt() ?: 0
                                        if (added > 0) {
                                            formSuccess = "Animal added!"
                                            formError = null
                                        } else {
                                            formError = "Animal already exists or failed."
                                            formSuccess = null
                                        }
                                    } catch (e: Exception) {
                                        formError = "Error: ${e.message}"
                                        formSuccess = null
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Animal")
                    }

                    formError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    formSuccess?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                }
            }
        }
    }
}

suspend fun parsePdfFile(inputStream: InputStream?): List<AnimalDetails> = withContext(Dispatchers.IO) {
    if (inputStream == null) throw Exception("Could not open file")
    val animals = mutableListOf<AnimalDetails>()
    try {
        val document = PDDocument.load(inputStream)
        val stripper = PDFTextStripper()
        val text = stripper.getText(document)
        document.close()
        val lines = text.split("\n")
        var parsing = false
        val regex = Regex("\\d+\\s+(\\S+)\\s+(\\S+)\\s+(M|F)\\s+(\\d{4}-\\d{2}-\\d{2})\\s+(RO\\d{10,})")
        for (line in lines) {
            if (!parsing) {
                if (line.contains("Cod identificare") && line.contains("Data nastere")) {
                    parsing = true
                }
                continue
            }
            val match = regex.find(line)
            if (match != null) {
                val species = match.groupValues[2]
                val gender = match.groupValues[3]
                val birthDate = match.groupValues[4]
                val animalId = match.groupValues[5]
                animals.add(AnimalDetails(animalId, gender, birthDate, species, false))
            }
        }
    } finally {
        inputStream.close()
    }
    animals
}