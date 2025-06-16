package com.example.farmerappfrontend

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimalDetailsScreen(animalId: String, token: String, navController: NavController) {
    var animalDetails by remember { mutableStateOf<AnimalDetails?>(null) }
    var animalEvents by remember { mutableStateOf<List<AnimalEvent>>(emptyList()) }

    var tempSpecies by remember { mutableStateOf("") }
    var tempBirthDate by remember { mutableStateOf("") }
    var tempProducesMilk by remember { mutableStateOf(false) }
    var tempGender by remember { mutableStateOf("") }

    var speciesOptions by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var speciesExpanded by remember { mutableStateOf(false) }
    var genderExpanded by remember { mutableStateOf(false) }

    var showEventDialog by remember { mutableStateOf(false) }
    var isUpdating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(animalId) {
        scope.launch {
            val response = RetrofitClient.apiService.getAnimalDetails(animalId, "Bearer $token")
            if (response.isSuccessful) {
                animalDetails = response.body()
                animalDetails?.let {
                    tempSpecies = it.species
                    tempBirthDate = it.birthDate
                    tempProducesMilk = it.producesMilk
                    tempGender = it.gender
                }
            }
            val speciesResponse = RetrofitClient.apiService.getSpecies()
            if (speciesResponse.isSuccessful) {
                speciesOptions = speciesResponse.body()
                    ?.map { it["value"]!! to it["label"]!! }
                    ?: emptyList()
            }
            val events = RetrofitClient.apiService.getEventsForAnimal(animalId, "Bearer $token")
            if (events.isSuccessful) {
                animalEvents = events.body() ?: emptyList()
            }
        }
    }

    fun updateAnimal() {
        val req = AnimalUpdateRequest(
            species = tempSpecies,
            birthDate = tempBirthDate,
            producesMilk = tempProducesMilk,
            gender = tempGender
        )
        scope.launch {
            isUpdating = true
            try {
                val res = RetrofitClient.apiService.updateAnimal(animalId, "Bearer $token", req)
                if (res.isSuccessful) {
                    animalDetails = res.body()
                    snackbarHostState.showSnackbar("Animal updated")
                } else {
                    snackbarHostState.showSnackbar("Update failed: ${res.message()}")
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Error: ${e.message}")
            } finally {
                isUpdating = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Animal Info", color = Color.White) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF6650a4))
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Button(
                onClick = { showEventDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6650a4))
            ) {
                Text("Add Event", color = Color.White)
            }
        }
    ) { innerPadding ->
        animalDetails?.let {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Animal ID: ${it.id}", fontWeight = FontWeight.Bold)

                // Species dropdown
                ExposedDropdownMenuBox(
                    expanded = speciesExpanded,
                    onExpandedChange = { speciesExpanded = !speciesExpanded }
                ) {
                    OutlinedTextField(
                        value = speciesOptions.find { opt -> opt.first == tempSpecies }?.second ?: tempSpecies,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Species") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = speciesExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = speciesExpanded,
                        onDismissRequest = { speciesExpanded = false }
                    ) {
                        speciesOptions.forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    tempSpecies = value
                                    speciesExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Gender dropdown
                ExposedDropdownMenuBox(
                    expanded = genderExpanded,
                    onExpandedChange = { genderExpanded = !genderExpanded }
                ) {
                    OutlinedTextField(
                        value = tempGender,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Gender") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = genderExpanded,
                        onDismissRequest = { genderExpanded = false }
                    ) {
                        listOf("M", "F").forEach { gender ->
                            DropdownMenuItem(
                                text = { Text(gender) },
                                onClick = {
                                    tempGender = gender
                                    genderExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Birthdate field
                OutlinedTextField(
                    value = tempBirthDate,
                    onValueChange = { tempBirthDate = it },
                    label = { Text("Birth Date (yyyy-MM-dd)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Produces Milk")
                    Switch(checked = tempProducesMilk, onCheckedChange = { tempProducesMilk = it })
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { updateAnimal() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUpdating
                ) {
                    Text("Update Animal")
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (animalEvents.isNotEmpty()) {
                    Text("Animal Events", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    animalEvents.sortedByDescending { it.eventDate }.forEach {
                        EventCard(it)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }

    if (showEventDialog && animalDetails != null) {
        AddEventDialog(
            onDismiss = { showEventDialog = false },
            onSave = { eventType, details ->
                showEventDialog = false
                postEvent(token, animalId, details) {
                    scope.launch {
                        val refresh = RetrofitClient.apiService.getEventsForAnimal(animalId, "Bearer $token")
                        if (refresh.isSuccessful) {
                            animalEvents = refresh.body() ?: emptyList()
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun InfoCard(label: String, value: String?, onClick: (() -> Unit)? = null) {
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value ?: "Not Available", fontSize = 16.sp)
        }
    }
}

@Composable
fun EventCard(event: AnimalEvent) {
    Card(
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Type: ${event.eventType}", fontWeight = FontWeight.SemiBold)
            Text("Date: ${event.eventDate}")
            Spacer(modifier = Modifier.height(6.dp))
            Text("Details:", fontWeight = FontWeight.Medium)
            event.details.forEach { (key, value) ->
                Text("$key: $value")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventDialog(
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

fun postEvent(token: String, animalId: String, payload: Map<String, Any>, onComplete: () -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = RetrofitClient.apiService.postEvent(animalId, "Bearer $token", payload)
            if (response.isSuccessful) {
                Log.d("Event", "Posted!")
                onComplete()
            } else {
                Log.e("Event", "Failed: ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("Event", "Exception: ${e.message}")
        }
    }
}

@Composable
fun CustomMilkToggle(isOn: Boolean, enabled: Boolean = true, onToggle: (() -> Unit)? = null) {
    Box(
        modifier = Modifier
            .width(56.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isOn) Color(0xFF4CAF50) else Color(0xFFE53935))
            .clickable(enabled = enabled && onToggle != null) { onToggle?.invoke() },
        contentAlignment = if (isOn) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .padding(2.dp)
                .background(Color.White, CircleShape)
        )
    }
}
