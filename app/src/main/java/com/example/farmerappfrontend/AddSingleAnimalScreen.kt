package com.example.farmerappfrontend

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.text.TextRange

import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSingleAnimalScreen(
    token: String,
    animalId: String,
    navController: NavController
) {
    var specie by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var producesMilk by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }
    var formSuccess by remember { mutableStateOf<String?>(null) }
    var speciesOptions by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.apiService.getSpecies()
            if (response.isSuccessful) {
                speciesOptions = response.body()?.map { it["value"]!! to it["label"]!! } ?: emptyList()
            } else {
                 formError = "Failed to load species: ${response.message()}"
            }
        } catch (e: Exception) {
            formError = "Error loading species: ${e.message}"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Animal", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Animal ID: $animalId", style = MaterialTheme.typography.titleMedium)

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
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
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

            var birthDateField by remember { mutableStateOf(TextFieldValue(birthDate)) }
            OutlinedTextField(
                value = birthDateField,
                onValueChange = { input ->
                    val currentDigits = birthDateField.text.filter { it.isDigit() }
                    val newDigits = input.text.filter { it.isDigit() }
                    val digits = if (newDigits.length < currentDigits.length) {
                        newDigits
                    } else {
                        newDigits.take(8)
                    }
                    val formatted = buildString {
                        digits.forEachIndexed { index, char ->
                            if (index == 4 || index == 6) append('-')
                            append(char)
                        }
                    }
                    birthDateField = TextFieldValue(
                        text = formatted,
                        selection = TextRange(formatted.length)
                    )
                    birthDate = formatted
                },
                label = { Text("Birth Date (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )

            var genderExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = genderExpanded,
                onExpandedChange = { genderExpanded = !genderExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = if (gender == "M") "M" else if (gender == "F") "F" else "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Gender") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                DropdownMenu(
                    expanded = genderExpanded,
                    onDismissRequest = { genderExpanded = false },
                    modifier = Modifier.exposedDropdownSize()
                ) {
                    DropdownMenuItem(
                        text = { Text("F") },
                        onClick = {
                            gender = "F"
                            genderExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("M") },
                        onClick = {
                            gender = "M"
                            producesMilk = false
                            genderExpanded = false
                        }
                    )
                }
            }

            if (gender == "F") {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Produces Milk", modifier = Modifier.weight(1f))
                    Switch(checked = producesMilk, onCheckedChange = { producesMilk = it })
                }
            }

            Button(
                onClick = {
                    if (specie.isBlank() || birthDate.isBlank() || gender.isBlank()) {
                        formError = "Species, Birth Date, and Gender are required."
                        formSuccess = null
                    } else {
                        scope.launch {
                            isLoading = true
                            try {
                                val animalDetails = AnimalDetails(
                                    id = animalId,
                                    gender = gender,
                                    birthDate = birthDate,
                                    species = specie,
                                    producesMilk = producesMilk
                                )
                                val response = RetrofitClient.apiService.addAnimal(token = "Bearer $token", animal = animalDetails)

                                if (response.isSuccessful) {
                                    formSuccess = "Animal added successfully!"
                                    formError = null
                                } else {
                                    formError = "Animal belongs to another user"
                                    formSuccess = null
                                }
                            } catch (e: Exception) {
                                formError = "Error: ${e.message}"
                                formSuccess = null
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text(if (isLoading) "Adding..." else "Add Animal")
            }

            formError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            formSuccess?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        }
    }
} 