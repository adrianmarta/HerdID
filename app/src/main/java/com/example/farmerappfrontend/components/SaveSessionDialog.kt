package com.example.farmerappfrontend.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

@Composable
fun SaveSessionDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var sessionName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save Counting Session") },
        text = {
            OutlinedTextField(
                value = sessionName,
                onValueChange = { sessionName = it },
                label = { Text("Session Name") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (sessionName.isNotBlank()) {
                        onSave(sessionName)
                    }
                },
                enabled = sessionName.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
} 