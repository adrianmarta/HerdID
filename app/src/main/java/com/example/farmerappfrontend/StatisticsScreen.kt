package com.example.farmerappfrontend

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.launch
import com.github.mikephil.charting.charts.BarChart as MPBarChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(navController: NavController) {
    val context = LocalContext.current
    val token = TokenManager.getToken(context)
    var folders by remember { mutableStateOf<List<Folder>>(emptyList()) }
    var selectedFolder by remember { mutableStateOf<Folder?>(null) }
    var statistics by remember { mutableStateOf<StatisticsResponse?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val customPurple = AndroidColor.parseColor("#6650a4")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Statistics",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.Folder, contentDescription = "Back", tint = Color.White)
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
                .padding(16.dp),
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
                statistics != null -> StatisticsContent(statistics!!)
            }
        }
    }

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

    LaunchedEffect(token, selectedFolder) {
        if (token.isNullOrBlank()) return@LaunchedEffect
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            try {
                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
                val response = RetrofitClient.apiService.getStatistics(authHeader, selectedFolder?.id)
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
        Text("Folder:")
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
fun StatisticsContent(stats: StatisticsResponse) {
    val total = stats.totalAnimalCount
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Milk Production", fontWeight = FontWeight.Bold)
                Text("Producers: ${stats.milkProducerCount} / $total")
                MPAndroidPieChart(milkProducers = stats.milkProducerCount, total = total)
            }
        }

        BirthsVsEligibleChart(stats)

        stats.diseaseCounts?.takeIf { it.isNotEmpty() }?.let { diseases ->
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Diseases", fontWeight = FontWeight.Bold)
                    MPAndroidBarChart(title = "Diseases", dataMap = diseases.mapValues { it.value ?: 0 })
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
                    MPAndroidBarChart(title = "Vaccinations", dataMap = vaccines.mapValues { it.value ?: 0 })
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
                    colors = listOf(AndroidColor.GREEN, AndroidColor.RED)
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
fun MPAndroidBarChart(title: String, dataMap: Map<String, Int>) {
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
                invalidate()
            }
        }
    )
}

@Composable
fun BirthsVsEligibleChart(stats: StatisticsResponse) {
    val availableYears = stats.birthsByYear?.keys?.sorted() ?: return
    var selectedYear by remember { mutableStateOf<String?>(null) }

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
                            selectedYear = year
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
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().height(220.dp),
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
                                invalidate()
                            }
                        }
                    )
                }
            }
        }
    }
}
