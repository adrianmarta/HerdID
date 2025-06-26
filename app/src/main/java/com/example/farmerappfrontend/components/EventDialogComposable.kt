package com.example.farmerappfrontend.ui.components

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.farmerappfrontend.RetrofitClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDialog(
    token: String,
    animalIds: List<String>,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
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

    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.apiService.getEventTypes()
            if (response.isSuccessful) {
                eventTypes = response.body() ?: emptyMap()
                if (eventTypes.isNotEmpty()) eventType = eventTypes.keys.first()
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
                errorMessage?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }

                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
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
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            eventTypes.forEach { (type, details) ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(type.replaceFirstChar { it.uppercase() })
                                            Text(details["description"] ?: "", style = MaterialTheme.typography.bodySmall)
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

                    eventTypes[eventType]?.get("description")?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }

                    OutlinedTextField(
                        value = eventDate,
                        onValueChange = { eventDate = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    when (eventType) {
                        "vaccination" -> {
                            OutlinedTextField(field1, { field1 = it }, label = { Text("Vaccine Name") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(field2, { field2 = it }, label = { Text("Dosage") }, modifier = Modifier.fillMaxWidth())
                        }
                        "sickness" -> {
                            OutlinedTextField(field1, { field1 = it }, label = { Text("Diagnosis") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(field2, { field2 = it }, label = { Text("Treatment") }, modifier = Modifier.fillMaxWidth())
                        }
                        "birth" -> {
                            OutlinedTextField(field1, { field1 = it }, label = { Text("Calf Gender") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(field2, { field2 = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
                        }
                        "death" -> {
                            OutlinedTextField(field1, { field1 = it }, label = { Text("Cause of Death") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(field2, { field2 = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val details = mutableMapOf<String, Any>(
                        "eventType" to eventType,
                        "eventDate" to eventDate
                    )
                    when (eventType) {
                        "vaccination" -> { details["vaccineName"] = field1; details["dosage"] = field2 }
                        "sickness" -> { details["diagnosis"] = field1; details["treatment"] = field2 }
                        "birth" -> { details["calfGender"] = field1; details["notes"] = field2 }
                        "death" -> { details["causeOfDeath"] = field1; details["notes"] = field2 }
                    }

                    scope.launch {
                        var success = 0
                        for (id in animalIds) {
                            try {
                                val res = RetrofitClient.apiService.postEvent(id, "Bearer $token", details)
                                if (res.isSuccessful) success++ else Log.e("EventDialog", "Failed for $id: ${res.message()}")
                            } catch (e: Exception) {
                                Log.e("EventDialog", "Exception for $id: ${e.message}")
                            }
                        }
                        onSuccess()
                    }
                },
                enabled = !isLoading && eventType.isNotEmpty() && eventDate.isNotEmpty() && field1.isNotEmpty() && field2.isNotEmpty()
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
