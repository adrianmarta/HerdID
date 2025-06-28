package com.example.farmerappfrontend

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
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
import java.time.temporal.ChronoUnit

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

    var showFilters by remember { mutableStateOf(false) }
    var selectedEventType by remember { mutableStateOf("") }
    var filterStartDate by remember { mutableStateOf("") }
    var filterEndDate by remember { mutableStateOf("") }
    var eventDateRange by remember { mutableStateOf(0f..1f) }
    var expanded by remember { mutableStateOf(false) }
    var allEventTypes by remember { mutableStateOf<List<String>>(emptyList()) }
    var eventTypesLoading by remember { mutableStateOf(true) }
    var eventTypesError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.apiService.getEventTypes()
            if (response.isSuccessful) {
                val types = response.body()?.keys?.toList()?.sorted() ?: emptyList()
                allEventTypes = types
            } else {
                eventTypesError = "Nu s-au putut încărca tipurile de evenimente"
            }
        } catch (e: Exception) {
            eventTypesError = "Eroare la încărcarea tipurilor: ${e.message}"
        } finally {
            eventTypesLoading = false
        }
    }

    var selectedEventIds by remember { mutableStateOf(setOf<String>()) }

    val eventDateRangeBounds = remember(animalEvents) {
        val dates = animalEvents.mapNotNull {
            try { LocalDate.parse(it.eventDate) } catch (e: Exception) { null }
        }
        if (dates.isEmpty()) Pair(LocalDate.now(), LocalDate.now())
        else Pair(dates.minOrNull()!!, dates.maxOrNull()!!)
    }

    // Inițializare epentru range și filtre
    LaunchedEffect(eventDateRangeBounds) {
        eventDateRange = 0f..1f
        filterStartDate = eventDateRangeBounds.first.format(DateTimeFormatter.ISO_DATE)
        filterEndDate = eventDateRangeBounds.second.format(DateTimeFormatter.ISO_DATE)
    }

    // Convert slider value to date
    fun sliderValueToEventDate(value: Float): LocalDate {
        val daysBetween = ChronoUnit.DAYS.between(eventDateRangeBounds.first, eventDateRangeBounds.second)
        val daysToAdd = (daysBetween * value).toLong()
        return eventDateRangeBounds.first.plusDays(daysToAdd)
    }
    // Convert date to slider value
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
        filterStartDate = sliderValueToEventDate(eventDateRange.start).format(DateTimeFormatter.ISO_DATE)
        filterEndDate = sliderValueToEventDate(eventDateRange.endInclusive).format(DateTimeFormatter.ISO_DATE)
    }

    // Filtrare evenimente
    val filteredEvents = animalEvents.filter { event ->
        (selectedEventType.isEmpty() || event.eventType == selectedEventType) &&
        (filterStartDate.isEmpty() || event.eventDate >= filterStartDate) &&
        (filterEndDate.isEmpty() || event.eventDate <= filterEndDate)
    }

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
                title = {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "HerdID Logo",
                        modifier = Modifier.size(40.dp).
                        clickable{navController.navigate("home/$token")}
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF6650a4))
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { showEventDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6650a4)),
                    elevation = ButtonDefaults.buttonElevation(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Event", color = Color.White)
                }
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val response = RetrofitClient.apiService.deleteEventsBulk(
                                    selectedEventIds.toList(),
                                    "Bearer $token"
                                )
                                if (response.isSuccessful) {
                                    animalEvents = animalEvents.filter { it.id !in selectedEventIds }
                                    selectedEventIds = emptySet()
                                    val msg = response.body()?.string() ?: "The events were succesfuly deleted."
                                    snackbarHostState.showSnackbar(msg)
                                } else {
                                    snackbarHostState.showSnackbar("Error: ${response.message()}")
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Error: ${e.message}")
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                    enabled = selectedEventIds.isNotEmpty(),
                    elevation = ButtonDefaults.buttonElevation(8.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Delete", color = Color.White)
                }
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
                Card(
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFB39DDB), Color(0xFFD1C4E9))
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {

                                Spacer(Modifier.width(8.dp))
                                Text("ID: ", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(it.id, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF5E35B1))
                            }
                            Spacer(Modifier.height(10.dp))
                            // Specie dropdown
                            Row(verticalAlignment = Alignment.CenterVertically) {

                                Spacer(Modifier.width(8.dp))
                                Text("Specie: ", fontWeight = FontWeight.SemiBold)
                                Box(Modifier.weight(1f)) {
                                    ExposedDropdownMenuBox(
                                        expanded = speciesExpanded,
                                        onExpandedChange = { speciesExpanded = !speciesExpanded }
                                    ) {
                                        OutlinedTextField(
                                            value = speciesOptions.find { opt -> opt.first == tempSpecies }?.second ?: tempSpecies,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = null,
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
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            // Sex dropdown
                            Row(verticalAlignment = Alignment.CenterVertically) {

                                Spacer(Modifier.width(8.dp))
                                Text("Sex: ", fontWeight = FontWeight.SemiBold)
                                Box(Modifier.weight(1f)) {
                                    ExposedDropdownMenuBox(
                                        expanded = genderExpanded,
                                        onExpandedChange = { genderExpanded = !genderExpanded }
                                    ) {
                                        OutlinedTextField(
                                            value = tempGender,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = null,
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
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            // Data nasterii
                            Row(verticalAlignment = Alignment.CenterVertically) {

                                Spacer(Modifier.width(8.dp))
                                Text("Data nasterii: ", fontWeight = FontWeight.SemiBold)
                                OutlinedTextField(
                                    value = tempBirthDate,
                                    onValueChange = { tempBirthDate = it },
                                    label = null,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            // Produce lapte
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Spacer(Modifier.width(8.dp))
                                Text("Produce lapte: ", fontWeight = FontWeight.SemiBold)
                                Switch(checked = tempProducesMilk, onCheckedChange = { tempProducesMilk = it })
                            }
                        }
                    }
                }
                // Buton de salvare
                Button(
                    onClick = { updateAnimal() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUpdating,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5E35B1)),
                    elevation = ButtonDefaults.buttonElevation(6.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Salveaza modificarile", color = Color.White)
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (animalEvents.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Evenimente animal", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF6650a4))
                        Button(
                            onClick = { showFilters = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E57C2)),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(6.dp))
                            Text("Show Filters", color = Color.White)
                        }
                    }
                    if (showFilters) {
                        AlertDialog(
                            onDismissRequest = { showFilters = false },
                            title = { Text("Filter Events") },
                            text = {
                                Column(Modifier.fillMaxWidth()) {
                                    // Dropdown tip eveniment
                                    Text("Event type:", fontWeight = FontWeight.SemiBold)
                                    var typeDropdownExpanded by remember { mutableStateOf(false) }
                                    if (eventTypesLoading) {
                                        CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                                    } else if (eventTypesError != null) {
                                        Text(eventTypesError!!, color = MaterialTheme.colorScheme.error)
                                    } else {
                                        ExposedDropdownMenuBox(
                                            expanded = typeDropdownExpanded,
                                            onExpandedChange = { typeDropdownExpanded = !typeDropdownExpanded }
                                        ) {
                                            OutlinedTextField(
                                                value = selectedEventType,
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("Choose type") },
                                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                                                modifier = Modifier.menuAnchor().fillMaxWidth()
                                            )
                                            ExposedDropdownMenu(
                                                expanded = typeDropdownExpanded,
                                                onDismissRequest = { typeDropdownExpanded = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("All") },
                                                    onClick = {
                                                        selectedEventType = ""
                                                        typeDropdownExpanded = false
                                                    }
                                                )
                                                allEventTypes.forEach { type ->
                                                    DropdownMenuItem(
                                                        text = { Text(type.replaceFirstChar { it.uppercase() }) },
                                                        onClick = {
                                                            selectedEventType = type
                                                            typeDropdownExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    // Data inceput si sfarsit + RangeSlider
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        OutlinedTextField(
                                            value = filterStartDate,
                                            onValueChange = { newValue ->
                                                filterStartDate = newValue
                                                if (isValidEventDateString(newValue)) {
                                                    updateEventDateRange(newValue, filterEndDate.ifEmpty { eventDateRangeBounds.second.format(DateTimeFormatter.ISO_DATE) })
                                                }
                                            },
                                            placeholder = { Text(eventDateRangeBounds.first.format(DateTimeFormatter.ISO_DATE)) },
                                            modifier = Modifier.weight(1f),
                                            textStyle = MaterialTheme.typography.bodyMedium,
                                            singleLine = true
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        OutlinedTextField(
                                            value = filterEndDate,
                                            onValueChange = { newValue ->
                                                filterEndDate = newValue
                                                if (isValidEventDateString(newValue)) {
                                                    updateEventDateRange(filterStartDate.ifEmpty { eventDateRangeBounds.first.format(DateTimeFormatter.ISO_DATE) }, newValue)
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
                            },
                            confirmButton = {
                                Button(
                                    onClick = { showFilters = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5E35B1))
                                ) {
                                    Text("Aplică filtrele", color = Color.White)
                                }
                            },
                            dismissButton = {
                                OutlinedButton(
                                    onClick = {
                                        selectedEventType = ""
                                        filterStartDate = ""
                                        filterEndDate = ""
                                        eventDateRange = 0f..1f
                                    }
                                ) {
                                    Text("Resetează")
                                }
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    filteredEvents.sortedByDescending { it.eventDate }.forEach { event ->
                        EventCard(
                            event = event,
                            selected = selectedEventIds.contains(event.id),
                            onClick = {
                                selectedEventIds = if (selectedEventIds.contains(event.id))
                                    selectedEventIds - event.id else selectedEventIds + event.id
                            }
                        )
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
fun EventCard(event: AnimalEvent, selected: Boolean = false, onClick: (() -> Unit)? = null) {
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFFD1C4E9) else Color(0xFFF3E5F5)
        ),
        border = if (selected) BorderStroke(2.dp, Color(0xFF7E57C2)) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Event, contentDescription = null, tint = Color(0xFF7E57C2))
                Spacer(Modifier.width(8.dp))
                Text("Tip: ", fontWeight = FontWeight.SemiBold)
                Text(event.eventType.capitalize(), fontWeight = FontWeight.Medium, color = Color(0xFF7E57C2))
            }
            Text("Data: ${event.eventDate}", fontWeight = FontWeight.Light)
            Spacer(modifier = Modifier.height(6.dp))
            Text("Detalii:", fontWeight = FontWeight.Medium)
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
