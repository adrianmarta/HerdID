package com.example.farmerappfrontend

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTransferScreen(navController: NavController, token: String) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var receiverId by remember { mutableStateOf("") }
    var folders by remember { mutableStateOf<List<Folder>>(emptyList()) }
    var selectedFolderId by remember { mutableStateOf<String?>(null) }
    var animalsInFolder by remember { mutableStateOf<List<AnimalDetails>>(emptyList()) }
    var isExpanded by remember { mutableStateOf(false) }


    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                val response = RetrofitClient.apiService.getFolders("Bearer $token")
                if (response.isSuccessful) {
                    folders = response.body() ?: emptyList()
                } else {
                    Toast.makeText(context, "Failed to load folders", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(selectedFolderId) {
        selectedFolderId?.let { folderId ->
            coroutineScope.launch {
                try {
                    val response = RetrofitClient.apiService.getAnimalsByFolderId( folderId,"Bearer $token")
                    if (response.isSuccessful) {
                        animalsInFolder = response.body() ?: emptyList()
                    } else {
                        Toast.makeText(context, "Failed to load animals for folder", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
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
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = receiverId,
                onValueChange = { receiverId = it },
                label = { Text("Receiver User ID") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            ExposedDropdownMenuBox(
                expanded = isExpanded,
                onExpandedChange = { isExpanded = !isExpanded },
            ) {
                OutlinedTextField(
                    value = folders.find { it.id == selectedFolderId }?.name ?: "Select a folder",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Folder") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded)
                    },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = isExpanded,
                    onDismissRequest = { isExpanded = false }
                ) {
                    folders.forEach { folder ->
                        DropdownMenuItem(
                            text = { Text(folder.name) },
                            onClick = {
                                selectedFolderId = folder.id
                                isExpanded = false
                            }
                        )
                    }
                }
            }


            Spacer(modifier = Modifier.height(16.dp))

            if (animalsInFolder.isNotEmpty()) {
                Text("Animals to be transferred:", style = MaterialTheme.typography.titleMedium)
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(animalsInFolder) { animal ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${animal.id}: ${animal.species}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (receiverId.isNotBlank() && selectedFolderId != null && animalsInFolder.isNotEmpty()) {
                        coroutineScope.launch {
                            try {
                                val userExistsResponse = RetrofitClient.apiService.checkUserExists(receiverId)
                                if (userExistsResponse.isSuccessful && userExistsResponse.body() == true) {
                                    val request = AnimalTransferRequest(
                                        receiverId = receiverId,
                                        animalIds = animalsInFolder.map { it.id }
                                    )
                                    val response = RetrofitClient.apiService.createTransfer("Bearer $token", request)
                                    if (response.isSuccessful) {
                                        Toast.makeText(context, "Transfer created successfully!", Toast.LENGTH_SHORT).show()
                                        navController.popBackStack()
                                    } else {
                                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                                        Toast.makeText(context, "Failed to create transfer: $errorBody", Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    Toast.makeText(context, "User ID does not exist.", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(context, "Please fill all fields and select a folder with animals.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = receiverId.isNotBlank() && selectedFolderId != null && animalsInFolder.isNotEmpty()
            ) {
                Text("Create Transfer")
            }
        }
    }
} 