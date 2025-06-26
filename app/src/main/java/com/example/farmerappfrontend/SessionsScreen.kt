package com.example.farmerappfrontend

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDFont
import com.tom_roush.pdfbox.pdmodel.font.PDType0Font
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.util.Matrix
import kotlinx.coroutines.launch
import java.io.OutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionsScreen(
    navController: NavController,
    cameraViewModel: CameraViewModel,
    folderCameraViewModel: FolderCameraViewModel
) {
    val context = LocalContext.current
    val token = TokenManager.getToken(LocalContext.current)
    var sessions by remember { mutableStateOf<List<CountingSession>>(emptyList()) }
    var folders by remember { mutableStateOf<List<Folder>>(emptyList()) }
    var currentPage by remember { mutableStateOf(0) }
    var totalPages by remember { mutableStateOf(1) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isExporting by remember { mutableStateOf(false) }
    var exportMessage by remember { mutableStateOf<String?>(null) }
    val sessionToExport = remember { mutableStateOf<CountingSession?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val createDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        uri?.let { outputUri ->
            sessionToExport.value?.let { session ->
                coroutineScope.launch {
                    isExporting = true
                    try {
                        context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                generateSessionPDFReport(
                                    context = context,
                                    outputStream = outputStream,
                                    token = token ?: "",
                                    session = session,
                                    folders = folders
                                )
                            }
                        }
                        exportMessage = "PDF for '${session.name}' exported successfully!"
                    } catch (e: Exception) {
                        exportMessage = "Error exporting PDF: ${e.message}"
                    } finally {
                        isExporting = false
                        sessionToExport.value = null
                    }
                }
            }
        } ?: run {
            exportMessage = "PDF export cancelled."
        }
    }

    if (isExporting) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Exporting PDF") },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Generating report...")
                }
            },
            confirmButton = {}
        )
    }

    exportMessage?.let {
        AlertDialog(
            onDismissRequest = { exportMessage = null },
            title = { Text("PDF Export") },
            text = { Text(it) },
            confirmButton = {
                Button(onClick = { exportMessage = null }) {
                    Text("OK")
                }
            }
        )
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
                        Text("Sessions")
                    }
                },

                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
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
            SessionList(
                sessions = sessions,
                folders = folders,
                navController = navController,
                token = token ?: "",
                cameraViewModel = cameraViewModel,
                folderCameraViewModel = folderCameraViewModel,
                onExportSession = { session ->
                    sessionToExport.value = session
                    createDocumentLauncher.launch("session_report_${session.name.replace(" ", "_")}.pdf")
                }
            )
            PaginationControls(
                currentPage = currentPage,
                totalPages = totalPages,
                onPreviousClicked = { if (currentPage > 0) currentPage-- },
                onNextClicked = { if (currentPage < totalPages - 1) currentPage++ }
            )
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

    LaunchedEffect(token, currentPage) {
        if (token.isNullOrBlank()) return@LaunchedEffect
        try {
            val sessionsResponse = RetrofitClient.apiService.getCountingSessions("Bearer $token", page = currentPage)
            if (sessionsResponse.isSuccessful) {
                val page = sessionsResponse.body()
                sessions = page?.content ?: emptyList()
                totalPages = page?.totalPages ?: 1
                if (page != null && page.number >= page.totalPages) {
                    currentPage = (page.totalPages - 1).coerceAtLeast(0)
                }
            } else {
                errorMessage = "Failed to load sessions: ${sessionsResponse.message()}"
            }
        } catch (e: Exception) {
            errorMessage = "Failed to load sessions: ${e.message}"
        }
    }
}

@Composable
fun SessionList(
    sessions: List<CountingSession>,
    folders: List<Folder>,
    navController: NavController,
    token: String,
    cameraViewModel: CameraViewModel,
    folderCameraViewModel: FolderCameraViewModel,
    onExportSession: (CountingSession) -> Unit
) {
    val scope = rememberCoroutineScope()
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Counting Sessions", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            if (sessions.isEmpty()) {
                Text("No saved sessions found.")
            } else {
                sessions.forEach { session ->
                    SessionItem(
                        session = session,
                        folderName = folders.find { it.id == session.folderId }?.name,
                        onClick = {
                            scope.launch {
                                if (session.folderId == null) {
                                    cameraViewModel.loadSession(session)
                                    navController.navigate("readAnimals/${token}")
                                } else {
                                    folderCameraViewModel.loadSession(session)
                                    navController.navigate("folderReadAnimals/${token}/${session.folderId}")
                                }
                            }
                        },
                        onExportClick = { onExportSession(session) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@SuppressLint("NewApi")
@Composable
fun SessionItem(session: CountingSession, folderName: String?, onClick: () -> Unit, onExportClick: () -> Unit) {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    val parsedDate = try {
        LocalDateTime.parse(session.readDate)
    } catch (e: Exception) {
        null
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onClick)
                    .padding(vertical = 16.dp)
            ) {
                Text(session.name, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                val datePart = parsedDate?.format(formatter) ?: "Unknown date"
                val countPart = "${session.readAnimalIds.size} animals"
                val contextPart = folderName?.let { "Folder: $it" } ?: "Animal List"
                Text(
                    text = "$datePart | $countPart | $contextPart",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onExportClick) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = "Export to PDF")
            }
        }
    }
}

@Composable
fun PaginationControls(
    currentPage: Int,
    totalPages: Int,
    onPreviousClicked: () -> Unit,
    onNextClicked: () -> Unit
) {
    if (totalPages > 1) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPreviousClicked,
                enabled = currentPage > 0
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Previous")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = "Page ${currentPage + 1} of $totalPages")
            Spacer(modifier = Modifier.width(16.dp))
            IconButton(
                onClick = onNextClicked,
                enabled = currentPage < totalPages - 1
            ) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Next")
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private suspend fun generateSessionPDFReport(
    context: Context,
    outputStream: OutputStream,
    token: String,
    session: CountingSession,
    folders: List<Folder>
) {
    // 1. Fetch data
    val userResponse = try {
        RetrofitClient.apiService.getUserProfile("Bearer $token")
    } catch (e: Exception) {
        null
    }
    val user = userResponse

    val allUserAnimalsResponse = RetrofitClient.apiService.getAnimalsByOwnerId("Bearer $token")
    val allUserAnimalIds = if (allUserAnimalsResponse.isSuccessful) {
        allUserAnimalsResponse.body()?.map { it.id }?.toSet() ?: emptySet()
    } else {
        emptySet()
    }

    // 2. Categorize animals IDs
    val newAnimalIds = session.readAnimalIds.filter { !allUserAnimalIds.contains(it) }
    val existingIds = session.readAnimalIds.filter { allUserAnimalIds.contains(it) }

    val animalDetailsMap = if (existingIds.isNotEmpty()) {
        try {
            RetrofitClient.apiService.getAnimalsByIds(existingIds, "Bearer $token")
                .body()?.associateBy { it.id } ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    } else {
        emptyMap()
    }

    val presentInFolderAnimals: List<AnimalDetails>
    var notInFolderAnimals: List<AnimalDetails> = emptyList()

    if (session.folderId == null) {
        presentInFolderAnimals = existingIds.mapNotNull { animalDetailsMap[it] }
    } else {
        val folderAnimalsResponse = RetrofitClient.apiService.getAnimalsByFolderId(session.folderId, "Bearer $token")
        val folderAnimalIds = if (folderAnimalsResponse.isSuccessful) {
            folderAnimalsResponse.body()?.map { it.id }?.toSet() ?: emptySet()
        } else {
            emptySet()
        }
        presentInFolderAnimals = existingIds.filter { folderAnimalIds.contains(it) }.mapNotNull { animalDetailsMap[it] }
        notInFolderAnimals = existingIds.filter { !folderAnimalIds.contains(it) }.mapNotNull { animalDetailsMap[it] }
    }


    // 3. Generate PDF
    PDFBoxResourceLoader.init(context)
    val document = PDDocument()
    var page = PDPage()
    document.addPage(page)
    var contentStream = PDPageContentStream(document, page)
    val font = PDType0Font.load(document, context.assets.open("fonts/arialuni.ttf"))
    val boldFont = PDType1Font.HELVETICA_BOLD
    val margin = 50f
    var yPosition = 750f

    fun ensureSpace(spaceNeeded: Float) {
        if (yPosition - spaceNeeded < margin) {
            contentStream.close()
            page = PDPage()
            document.addPage(page)
            contentStream = PDPageContentStream(document, page)
            yPosition = 750f
        }
    }

    fun writeText(text: String, x: Float, font: PDFont, fontSize: Float) {
        ensureSpace(fontSize + 5)
        contentStream.beginText()
        contentStream.setFont(font, fontSize)
        contentStream.newLineAtOffset(x, yPosition)
        contentStream.showText(text)
        contentStream.endText()
        yPosition -= (fontSize + 5)
    }

    fun String.replaceRomanianChars(): String {
        return this
            .replace('ț', 't').replace('ș', 's').replace('ă', 'a')
            .replace('î', 'i').replace('â', 'a').replace('Ț', 'T')
            .replace('Ș', 'S').replace('Ă', 'A').replace('Î', 'I').replace('Â', 'A')
    }

    fun drawAnimalTable(title: String, animals: List<AnimalDetails>) {
        if (animals.isEmpty()) return

        ensureSpace(50f)
        yPosition -= 20f
        writeText(title, margin, boldFont, 14f)

        val tableWidth = PDPage().mediaBox.width - 2 * margin
        val rowHeight = 20f
        val headerHeight = 25f
        val columnWidths = floatArrayOf(30f, 80f, 80f, 40f, 100f, tableWidth - 330f)
        val headers = listOf("Nr.", "Specie", "Rasa", "Sex", "Data nastere", "Cod identificare")

        // Draw header
        ensureSpace(headerHeight)
        var currentX = margin
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 10f)
        for (i in headers.indices) {
            contentStream.beginText()
            contentStream.setTextMatrix(Matrix.getTranslateInstance(currentX + 5f, yPosition - 15f))
            contentStream.showText(headers[i].replaceRomanianChars())
            contentStream.endText()
            currentX += columnWidths[i]
        }
        contentStream.setLineWidth(0.5f)
        contentStream.moveTo(margin, yPosition - headerHeight)
        contentStream.lineTo(margin + tableWidth, yPosition - headerHeight)
        contentStream.stroke()
        currentX = margin
        for (width in columnWidths) {
            contentStream.moveTo(currentX, yPosition - headerHeight)
            contentStream.lineTo(currentX, yPosition - headerHeight)
            contentStream.stroke()
            currentX += width
        }
        contentStream.moveTo(margin + tableWidth, yPosition)
        contentStream.lineTo(margin + tableWidth, yPosition - headerHeight)
        contentStream.stroke()
        yPosition -= headerHeight

        contentStream.setFont(PDType1Font.HELVETICA, 8f)
        animals.forEachIndexed { index, animal ->
            ensureSpace(rowHeight)
            val rowY = yPosition - 15f
            var currentColumnX = margin

            // Nr.
            contentStream.beginText(); contentStream.setTextMatrix(Matrix.getTranslateInstance(currentColumnX + 5f, rowY)); contentStream.showText((index + 1).toString()); contentStream.endText(); currentColumnX += columnWidths[0]
            // Specie
            contentStream.beginText(); contentStream.setTextMatrix(Matrix.getTranslateInstance(currentColumnX + 5f, rowY)); contentStream.showText("Ovine".replaceRomanianChars()); contentStream.endText(); currentColumnX += columnWidths[1]
            // Rasa
            contentStream.beginText(); contentStream.setTextMatrix(Matrix.getTranslateInstance(currentColumnX + 5f, rowY)); contentStream.showText(animal.species.replaceRomanianChars()); contentStream.endText(); currentColumnX += columnWidths[2]
            // Sex
            contentStream.beginText(); contentStream.setTextMatrix(Matrix.getTranslateInstance(currentColumnX + 5f, rowY)); contentStream.showText(animal.gender.replaceRomanianChars()); contentStream.endText(); currentColumnX += columnWidths[3]
            // Data nastere
            contentStream.beginText(); contentStream.setTextMatrix(Matrix.getTranslateInstance(currentColumnX + 5f, rowY)); contentStream.showText(animal.birthDate.replaceRomanianChars()); contentStream.endText(); currentColumnX += columnWidths[4]
            // Cod identificare
            contentStream.beginText(); contentStream.setTextMatrix(Matrix.getTranslateInstance(currentColumnX + 5f, rowY)); contentStream.showText(animal.id.replaceRomanianChars()); contentStream.endText()

            contentStream.setLineWidth(0.5f)
            contentStream.moveTo(margin, yPosition - rowHeight)
            contentStream.lineTo(margin + tableWidth, yPosition - rowHeight)
            contentStream.stroke()
            currentColumnX = margin
            for (width in columnWidths) {
                contentStream.moveTo(currentColumnX, yPosition); contentStream.lineTo(currentColumnX, yPosition - rowHeight); contentStream.stroke(); currentColumnX += width
            }
            contentStream.moveTo(margin + tableWidth, yPosition); contentStream.lineTo(margin + tableWidth, yPosition - rowHeight); contentStream.stroke()
            yPosition -= rowHeight
        }
        yPosition -= 20
    }

    fun drawIdList(title: String, animalIds: List<String>) {
        if (animalIds.isEmpty()) return
        ensureSpace(50f)
        yPosition -= 20f
        writeText(title, 50f, boldFont, 14f)
        yPosition -= 5
        for (id in animalIds) {
            ensureSpace(15f)
            writeText("- $id", 60f, font, 10f)
        }
        yPosition -= 20
    }

    // Write Report Header
    writeText("Session Report", 50f, boldFont, 18f)
    yPosition -= 10
    writeText("Session Name: ${session.name}", 50f, font, 12f)
    writeText("Date: ${LocalDateTime.parse(session.readDate).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}", 50f, font, 12f)
    user?.let {
        writeText("User: ${it.name} (${it.id})", 50f, font, 12f)
    }
    session.folderId?.let {
        val folderName = folders.find { f -> f.id == it }?.name ?: "Unknown"
        writeText("Folder: $folderName", 50f, font, 12f)
    }
    yPosition -= 20

    // Draw Content
    if (session.folderId == null) {
        drawAnimalTable("Present Animals", presentInFolderAnimals)
    } else {
        drawAnimalTable("Present in Folder", presentInFolderAnimals)
        drawAnimalTable("Present Globally (Not in Folder)", notInFolderAnimals)
    }
    drawIdList("New Animals", newAnimalIds)


    contentStream.close()
    document.save(outputStream)
    document.close()
}