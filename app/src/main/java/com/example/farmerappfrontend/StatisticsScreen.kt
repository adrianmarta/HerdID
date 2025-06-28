package com.example.farmerappfrontend

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.clickable
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.browser.trusted.Token
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.github.mikephil.charting.charts.PieChart as MPPieChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.data.Entry
import kotlinx.coroutines.launch
import com.github.mikephil.charting.charts.BarChart as MPBarChart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Year
import java.util.Calendar
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(navController: NavController,token:String) {
    val context = LocalContext.current
    val token = TokenManager.getToken(context)
    var folders by remember { mutableStateOf<List<Folder>>(emptyList()) }
    var selectedFolder by remember { mutableStateOf<Folder?>(null) }
    var statistics by remember { mutableStateOf<StatisticsResponse?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val customPurple = AndroidColor.parseColor("#6650a4")
    var selectedYear by remember { mutableStateOf<String?>(null) }

    var showAnimalDialog by remember { mutableStateOf(false) }
    var selectedEventType by remember { mutableStateOf<String?>(null) }
    var selectedEventName by remember { mutableStateOf<String?>(null) }
    var selectedEventAnimals by remember { mutableStateOf<List<AnimalDetails>>(emptyList()) }
    var isLoadingAnimals by remember { mutableStateOf(false) }
    var isGeneratingFolder by remember { mutableStateOf(false) }

    @SuppressLint("NewApi")
    fun loadAnimalsByEvent(eventType: String, eventName: String?) {
        selectedEventType = eventType
        selectedEventName = eventName
        showAnimalDialog = true
        isLoadingAnimals = true

        coroutineScope.launch {
            try {
                val response = when (eventType) {
                    "birth" -> {
                        val year = eventName?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
                        val startDate = "$year-01-01"
                        val endDate = "$year-12-31"
                        RetrofitClient.apiService.getAnimalsByBirthDate(
                            startDate,
                            endDate,
                            "Bearer $token"
                        )
                    }

                    "sickness" -> {
                        RetrofitClient.apiService.getAnimalsBySickness(
                            eventName ?: "",
                            "Bearer $token"
                        )
                    }

                    "vaccination" -> {
                        RetrofitClient.apiService.getAnimalsByVaccination(
                            eventName ?: "",
                            "Bearer $token"
                        )
                    }

                    else -> null
                }

                if (response?.isSuccessful == true) {
                    selectedEventAnimals = response.body() ?: emptyList()
                } else {
                    selectedEventAnimals = emptyList()
                }
            } catch (e: Exception) {
                selectedEventAnimals = emptyList()
            } finally {
                isLoadingAnimals = false
            }
        }
    }
    fun generateFolderAndNavigate(eventType: String, eventName: String?) {
        isGeneratingFolder = true

        coroutineScope.launch {
            try {
                val folderName = when (eventType) {
                    "birth" -> "Births $eventName"
                    "sickness" -> "Sickness: $eventName"
                    "vaccination" -> "Vaccination: $eventName"
                    else -> "Event Folder"
                }

                val folderRequest = FolderRequest(name = folderName)
                val createResponse =
                    RetrofitClient.apiService.createFolder("Bearer $token", folderRequest)

                if (createResponse.isSuccessful) {
                    val newFolderId = createResponse.body()?.id
                    if (newFolderId != null && selectedEventAnimals.isNotEmpty()) {
                        val addResponse = RetrofitClient.apiService.addAnimalsToFolder(
                            folderId = newFolderId,
                            animalIds = selectedEventAnimals.map { it.id },
                            authorization = "Bearer $token"
                        )

                        if (addResponse.isSuccessful) {
                            showAnimalDialog = false
                            navController.navigate("folder/${newFolderId}/animals/$token")
                        }
                    }
                }
            } catch (e: Exception) {

            } finally {
                isGeneratingFolder = false
            }
        }
    }
    fun checkFolderAndNavigate(eventType: String, eventName: String?) {
        val expectedFolderName = when (eventType) {
            "birth" -> "Births $eventName"
            "sickness" -> "Sickness: $eventName"
            "vaccination" -> "Vaccination: $eventName"
            else -> ""
        }

        val existingFolder = folders.find { it.name == expectedFolderName }
        if (existingFolder != null) {
            showAnimalDialog = false
            navController.navigate("folder/${existingFolder.id}/animals/$token")
        } else {
            generateFolderAndNavigate(eventType, eventName)
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
                        Text("Statistics")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(customPurple),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            FolderDropdown(
                folders = folders,
                selectedFolder = selectedFolder,
                onFolderSelected = { selectedFolder = it }
            )

            when {
                isLoading -> LoadingState()
                errorMessage != null -> ErrorState(errorMessage!!)
                statistics != null -> StatisticsContent(
                    statistics!!,
                    selectedYear,
                    onYearSelected = { selectedYear = it },
                    onChartClick = { eventType, eventName ->
                        loadAnimalsByEvent(
                            eventType,
                            eventName
                        )
                    }
                )
            }
        }
    }

    // Animal Preview Dialog
    if (showAnimalDialog) {
        AlertDialog(
            onDismissRequest = {
                showAnimalDialog = false
                selectedEventAnimals = emptyList()
            },
            title = {
                Text(
                    when (selectedEventType) {
                        "birth" -> "Animals born in $selectedEventName"
                        "sickness" -> "Animals with sickness: $selectedEventName"
                        "vaccination" -> "Animals vaccinated with: $selectedEventName"
                        else -> "Animals"
                    }
                )
            },
            text = {
                if (isLoadingAnimals) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    Column {
                        Text(
                            text = "Total animals: ${selectedEventAnimals.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        if (selectedEventAnimals.isEmpty()) {
                            Text("No animals found")
                        } else {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 300.dp)
                            ) {
                                items(selectedEventAnimals.take(10)) { animal ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        elevation = CardDefaults.cardElevation(2.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp)
                                        ) {
                                            Text(
                                                text = "ID: ${animal.id}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "Species: ${animal.species}",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            Text(
                                                text = "Birth Date: ${animal.birthDate}",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }

                            if (selectedEventAnimals.size > 10) {
                                Text(
                                    text = "... and ${selectedEventAnimals.size - 10} more",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            selectedEventType?.let { type ->
                                selectedEventName?.let { name ->
                                    checkFolderAndNavigate(type, name)
                                }
                            }
                        },
                        enabled = !isGeneratingFolder && selectedEventAnimals.isNotEmpty()
                    ) {
                        if (isGeneratingFolder) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generating...")
                        } else {
                            Icon(Icons.Default.Folder, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Go to Folder")
                        }
                    }
                    Button(
                        onClick = {
                            showAnimalDialog = false
                            selectedEventAnimals = emptyList()
                        }
                    ) {
                        Text("Close")
                    }
                }
            }
        )
    }

    // Fetch folders
    LaunchedEffect(token) {
        if (token.isNullOrBlank()) {
            errorMessage = "Authentication token not found"
            return@LaunchedEffect
        }
        try {
            val folderResponse = RetrofitClient.apiService.getFolders("Bearer $token")
            if (folderResponse.isSuccessful) {
                folders = folderResponse.body() ?: emptyList()
            } else {
                errorMessage = "Failed to load folders: ${folderResponse.message()}"
            }
        } catch (e: Exception) {
            errorMessage = "Failed to load initial data: ${e.message}"
        }
    }

    // Fetch statistics
    LaunchedEffect(token, selectedFolder) {
        if (token.isNullOrBlank()) return@LaunchedEffect
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            try {
                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
                val response =
                    RetrofitClient.apiService.getStatistics(authHeader, selectedFolder?.id)
                if (response.isSuccessful) {
                    statistics = response.body()
                } else {
                    errorMessage = when (response.code()) {
                        401 -> "Authentication failed"
                        403 -> "Access denied"
                        else -> "Error fetching statistics: ${response.code()}"
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }


}

@Composable
fun FolderDropdown(folders: List<Folder>, selectedFolder: Folder?, onFolderSelected: (Folder?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Folder")
        Spacer(Modifier.width(8.dp))
        Box {
            OutlinedButton(onClick = { expanded = true }) {
                Text(selectedFolder?.name ?: "All", maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text("All") }, onClick = {
                    onFolderSelected(null)
                    expanded = false
                })
                folders.forEach { folder ->
                    DropdownMenuItem(text = { Text(folder.name) }, onClick = {
                        onFolderSelected(folder)
                        expanded = false
                    })
                }
            }
        }
    }
}

@Composable
fun LoadingState() {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorState(message: String) {
    Text(message, color = MaterialTheme.colorScheme.error)
}

@Composable
fun StatisticsContent(stats: StatisticsResponse, selectedYear: String?, onYearSelected: (String) -> Unit, onChartClick: (String, String?) -> Unit) {
    val availableYears = stats.birthsByYear?.keys?.sorted() ?: emptyList()
    LaunchedEffect(availableYears) {
        if (availableYears.isNotEmpty() && (selectedYear == null || selectedYear !in availableYears)) {
            onYearSelected(availableYears.first())
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Milk Production", fontWeight = FontWeight.Bold)
                Text("Producers: ${stats.milkProducerCount} / ${stats.totalAnimalCount}")
                MPAndroidPieChart(milkProducers = stats.milkProducerCount, total = stats.totalAnimalCount)
            }
        }

        BirthsVsEligibleChart(stats, selectedYear, onYearSelected, onChartClick)

        stats.diseaseCounts?.takeIf { it.isNotEmpty() }?.let { diseases ->
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Diseases", fontWeight = FontWeight.Bold)
                    MPAndroidBarChart(title = "Diseases", dataMap = diseases.mapValues { it.value ?: 0 }, onChartClick = { eventType, eventName -> onChartClick(eventType, eventName) })
                }
            }
        }

        stats.vaccineCounts?.takeIf { it.isNotEmpty() }?.let { vaccines ->
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Vaccinations", fontWeight = FontWeight.Bold)
                    MPAndroidBarChart(title = "Vaccinations", dataMap = vaccines.mapValues { it.value ?: 0 }, onChartClick = { eventType, eventName -> onChartClick(eventType, eventName) })
                }
            }
        }
    }
}

@Composable
fun MPAndroidPieChart(milkProducers: Int, total: Int) {
    AndroidView(
        modifier = Modifier.fillMaxWidth().height(220.dp),
        factory = { context ->
            MPPieChart(context).apply {
                val entries = listOf(
                    PieEntry(milkProducers.toFloat(), "Milk"),
                    PieEntry((total - milkProducers).toFloat(), "No Milk")
                )
                val dataSet = PieDataSet(entries, "").apply {
                    colors = listOf(AndroidColor.BLUE, AndroidColor.RED)
                    valueTextColor = AndroidColor.WHITE
                    valueTextSize = 14f
                }
                data = PieData(dataSet)
                description = Description().apply { text = "" }
                legend.isEnabled = true
                invalidate()
            }
        }
    )
}

@Composable
fun MPAndroidBarChart(title: String, dataMap: Map<String, Int>, onChartClick: (String, String?) -> Unit) {
    val eventType = when (title) {
        "Diseases" -> "sickness"
        "Vaccinations" -> "vaccination"
        else -> ""
    }
    
    AndroidView(
        modifier = Modifier.fillMaxWidth().height(250.dp),
        factory = { context ->
            MPBarChart(context).apply {
                val entries = dataMap.entries.mapIndexed { index, entry ->
                    BarEntry(index.toFloat(), entry.value.toFloat())
                }
                val labels = dataMap.keys.toList()
                val dataSet = BarDataSet(entries, title).apply {
                    color = AndroidColor.BLUE
                    valueTextColor = AndroidColor.BLACK
                    valueTextSize = 12f
                }
                data = BarData(dataSet)
                xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                xAxis.granularity = 1f
                xAxis.setDrawGridLines(false)
                axisLeft.setDrawGridLines(false)
                axisRight.isEnabled = false
                description = Description().apply { text = "" }
                legend.isEnabled = false
                
                setOnChartValueSelectedListener(object : com.github.mikephil.charting.listener.OnChartValueSelectedListener {
                    override fun onValueSelected(e: Entry?, h: Highlight?) {
                        e?.let { entry ->
                            val index = entry.x.toInt()
                            if (index < labels.size) {
                                onChartClick(eventType, labels[index])
                            }
                        }
                    }
                    
                    override fun onNothingSelected() {
                    }
                })
                
                invalidate()
            }
        }
    )
}

@Composable
fun BirthsVsEligibleChart(
    stats: StatisticsResponse,
    selectedYear: String?,
    onYearSelected: (String) -> Unit,
    onChartClick: (String, String?) -> Unit
) {
    val availableYears = stats.birthsByYear?.keys?.sorted() ?: return

    val femaleEligible = selectedYear?.let { stats.eligibleFemalesByYear?.get(it) } ?: 0
    val femaleBirths = selectedYear?.let { stats.femalesGaveBirthByYear?.get(it)?.size } ?: 0

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Select Year:")
            Spacer(Modifier.width(8.dp))
            var expanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { expanded = true }) {
                    Text(selectedYear ?: "Year")
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    availableYears.forEach { year ->
                        DropdownMenuItem(text = { Text(year) }, onClick = {
                            onYearSelected(year)
                            expanded = false
                        })
                    }
                }
            }
        }

        if (selectedYear != null && femaleEligible > 0) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Female Births in $selectedYear vs Eligible", fontWeight = FontWeight.Bold)
                    key(selectedYear) {
                        AndroidView(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            factory = { context ->
                                MPPieChart(context).apply {
                                    val nonBirths = (femaleEligible - femaleBirths).coerceAtLeast(0)
                                    val entries = listOf(
                                        PieEntry(femaleBirths.toFloat(), "Gave Birth"),
                                        PieEntry(nonBirths.toFloat(), "Did Not")
                                    )
                                    val dataSet = PieDataSet(entries, "").apply {
                                        colors = listOf(AndroidColor.MAGENTA, AndroidColor.LTGRAY)
                                        valueTextColor = AndroidColor.BLACK
                                        valueTextSize = 14f
                                    }
                                    data = PieData(dataSet)
                                    description = Description().apply { text = "" }
                                    legend.isEnabled = true
                                    setOnChartValueSelectedListener(object : com.github.mikephil.charting.listener.OnChartValueSelectedListener {
                                        override fun onValueSelected(e: Entry?, h: Highlight?) {
                                            onChartClick("birth", selectedYear)
                                        }
                                        override fun onNothingSelected() {}
                                    })
                                    invalidate()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
