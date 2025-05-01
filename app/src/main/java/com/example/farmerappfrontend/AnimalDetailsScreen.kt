package com.example.farmerappfrontend

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimalDetailsScreen(animalId: String, token: String, navController: NavController) {
    var animalDetails by remember { mutableStateOf<AnimalDetails?>(null) }
    var animalEvents by remember { mutableStateOf<List<AnimalEvent>>(emptyList()) }
    var showEventDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(animalId) {
        scope.launch {
            try {
                val animalResponse = RetrofitClient.apiService.getAnimalDetails(animalId, "Bearer $token")
                if (animalResponse.isSuccessful) {
                    animalDetails = animalResponse.body()
                }
                val eventsResponse = RetrofitClient.apiService.getEventsForAnimal(animalId, "Bearer $token")
                if (eventsResponse.isSuccessful) {
                    animalEvents = eventsResponse.body() ?: emptyList()
                }
            } catch (e: Exception) {
                Log.e("AnimalDetails", "Error: ${e.message}")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Animal Info") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(MaterialTheme.colorScheme.primary)
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                Button(
                    onClick = { showEventDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Add Event", fontSize = 14.sp)
                }

                Button(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Back", fontSize = 14.sp)
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            animalDetails?.let { animal ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Text(
                        text = "Animal ID: ${animal.id}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    InfoCard("Gender", animal.gender ?: "Unknown")
                    InfoCard("Birth Date", animal.birthDate ?: "Unknown")
                    InfoCard("Species", animal.species ?: "Unknown")
                    InfoCard("Produces Milk", if (animal.producesMilk) "Yes" else "No")

                    if (animalEvents.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "Animal Events",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        animalEvents.sortedByDescending { it.eventDate }.forEach { event ->
                            EventCard(event)
                        }
                    }
                }
            } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }

    if (showEventDialog && animalDetails != null) {
        AddEventDialog(
            onDismiss = { showEventDialog = false },
            onSave = { eventType, details ->
                showEventDialog = false
                postEvent(token, animalDetails!!.id, details) {
                    scope.launch {
                        try {
                            val refreshedEvents = RetrofitClient.apiService.getEventsForAnimal(animalDetails!!.id, "Bearer $token")
                            if (refreshedEvents.isSuccessful) {
                                animalEvents = refreshedEvents.body() ?: emptyList()
                            }
                        } catch (e: Exception) {
                            Log.e("AnimalDetails", "Reload Error: ${e.message}")
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun InfoCard(label: String, value: String?) {
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
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
    var eventType by remember { mutableStateOf("vaccination") }
    var eventDate by remember { mutableStateOf("") }
    var field1 by remember { mutableStateOf("") }
    var field2 by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Animal Event") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Event Type:")
                var expanded by remember { mutableStateOf(false) }
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


                OutlinedTextField(
                    value = eventDate,
                    onValueChange = { eventDate = it },
                    label = { Text("Date (YYYY-MM-DD)") }
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
                }
                details["eventType"] = eventType
                details["eventDate"] = eventDate
                onSave(eventType, details)
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
