package com.example.farmerappfrontend

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.res.painterResource
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.PictureAsPdf
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import java.io.OutputStream
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransfersScreen(navController: NavController, token: String) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Incoming", "Sent", "History")

    var pendingTransfers by remember { mutableStateOf<List<AnimalTransfer>>(emptyList()) }
    var sentTransfers by remember { mutableStateOf<List<AnimalTransfer>>(emptyList()) }
    var receivedTransfers by remember { mutableStateOf<List<AnimalTransfer>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var selectedTransferForExport by remember { mutableStateOf<AnimalTransfer?>(null) }
    val createDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let {
            selectedTransferForExport?.let { transfer ->
                coroutineScope.launch {
                    try {
                        PDFBoxResourceLoader.init(context)
                        context.contentResolver.openOutputStream(it)?.use { outputStream ->
                            generateTransferPDF(context, outputStream, transfer, token)
                        }
                        Toast.makeText(context, "PDF exported successfully!", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error exporting PDF: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    fun fetchTransfers() {
        coroutineScope.launch {
            try {
                val pendingResponse = RetrofitClient.apiService.getPendingTransfers("Bearer $token")
                if (pendingResponse.isSuccessful) {
                    pendingTransfers = pendingResponse.body() ?: emptyList()
                }
                val sentResponse = RetrofitClient.apiService.getSentTransfers("Bearer $token")
                if (sentResponse.isSuccessful) {
                    sentTransfers = sentResponse.body() ?: emptyList()
                }
                val receivedResponse = RetrofitClient.apiService.getReceivedTransfers("Bearer $token")
                if (receivedResponse.isSuccessful) {
                    receivedTransfers = receivedResponse.body() ?: emptyList()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load transfers: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchTransfers()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "HerdID Logo",
                        modifier = Modifier
                            .size(40.dp)
                            .clickable { navController.navigate("home/$token") },
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Sessions")
                } },

                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(MaterialTheme.colorScheme.primary)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                navController.navigate("createTransfer/$token")
            }) {
                Icon(Icons.Default.Add, contentDescription = "Create Transfer")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> TransferList(
                    transfers = pendingTransfers,
                    isIncoming = true,
                    onAccept = { transferId ->
                        coroutineScope.launch {
                            try {
                                val response = RetrofitClient.apiService.acceptTransfer("Bearer $token", transferId)
                                if (response.isSuccessful) {
                                    Toast.makeText(context, "Transfer accepted!", Toast.LENGTH_SHORT).show()
                                    fetchTransfers()
                                } else {
                                    Toast.makeText(context, "Failed to accept transfer: ${response.body()}", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
                1 -> TransferList(transfers = sentTransfers.filter { it.status == TransferStatus.PENDING })
                2 -> TransferList(
                    transfers = (sentTransfers.filter { it.status == TransferStatus.COMPLETED } + receivedTransfers),
                    onExport = { transfer ->
                        selectedTransferForExport = transfer
                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                        createDocumentLauncher.launch("transfer_report_${transfer.id}_$timestamp.pdf")
                    }
                )
            }
        }
    }
}

@Composable
fun TransferList(
    transfers: List<AnimalTransfer>,
    isIncoming: Boolean = false,
    onAccept: ((String) -> Unit)? = null,
    onExport: ((AnimalTransfer) -> Unit)? = null
) {
    if (transfers.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No transfers to show.")
        }
    } else {
        LazyColumn(modifier = Modifier.padding(16.dp)) {
            items(transfers) { transfer ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                if (isIncoming) {
                                    Text("From: ${transfer.senderId}", style = MaterialTheme.typography.titleMedium)
                                } else {
                                    Text("To: ${transfer.receiverId}", style = MaterialTheme.typography.titleMedium)
                                }
                                Text("Status: ${transfer.status}", style = MaterialTheme.typography.bodyMedium)
                            }
                            if (transfer.status == TransferStatus.COMPLETED && onExport != null) {
                                IconButton(onClick = { onExport(transfer) }) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Number of animals: ${transfer.animalIds.size}", style = MaterialTheme.typography.bodyMedium)
                        Text("Date: ${formatTransferDate(transfer.transferDate)}", style = MaterialTheme.typography.bodySmall)

                        if (isIncoming && transfer.status == TransferStatus.PENDING && onAccept != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { onAccept(transfer.id) }) {
                                Text("Accept")
                            }
                        }
                    }
                }
            }
        }
    }
}

fun String.replaceRomanianChars(): String {
    return this
        .replace('ț', 't')
        .replace('ș', 's')
        .replace('ă', 'a')
        .replace('î', 'i')
        .replace('â', 'a')
        .replace('Ț', 'T')
        .replace('Ș', 'S')
        .replace('Ă', 'A')
        .replace('Î', 'I')
        .replace('Â', 'A')
}

@SuppressLint("NewApi")
private fun formatTransferDate(dateString: String): String {
    return try {
        val instant = try { // Try ISO_INSTANT first
            java.time.Instant.parse(dateString)
        } catch (_: Exception) {
            null
        }
        val localDateTime = if (instant != null) {
            LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
        } else {
            LocalDateTime.parse(dateString, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"))
        }
        localDateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
    } catch (e: Exception) {
        dateString
    }
}

private suspend fun generateTransferPDF(
    context: Context,
    outputStream: OutputStream,
    transfer: AnimalTransfer,
    token: String
) {
    // Fetch details for sender, receiver, and animals
    val sender = RetrofitClient.apiService.getUserProfile( "Bearer $token")
    val animals = RetrofitClient.apiService.getAnimalsByIds(transfer.animalIds, "Bearer $token").body() ?: emptyList()

    val document = PDDocument()
    val page = PDPage()
    document.addPage(page)
    val contentStream = PDPageContentStream(document, page)
    val margin = 50f
    var yPosition = page.mediaBox.height - margin
    val tableMargin = margin
    val rowHeight = 20f
    val headerHeight = 25f
    val cellMargin = 5f
    val tableWidth = PDPage().mediaBox.width - 2 * margin
    val cellWidths = floatArrayOf(120f, 80f, 80f, 100f)
    val headers = listOf("Animal ID", "Species", "Gender", "Birth Date")

    // Title
    contentStream.beginText()
    contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18f)
    contentStream.newLineAtOffset(margin, yPosition)
    contentStream.showText("Transfer Report".replaceRomanianChars())
    contentStream.endText()
    yPosition -= 30f

    // User Details
    contentStream.beginText()
    contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12f)
    contentStream.newLineAtOffset(margin, yPosition)
    contentStream.showText(("From: ${sender?.name ?: "N/A"} (ID: ${transfer.senderId})").replaceRomanianChars())
    contentStream.endText()
    yPosition -= 20f

    contentStream.beginText()
    contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12f)
    contentStream.newLineAtOffset(margin, yPosition)
    contentStream.showText(("TO: ID: ${transfer.receiverId}").replaceRomanianChars())
    contentStream.endText()
    yPosition -= 20f

    contentStream.beginText()
    contentStream.setFont(PDType1Font.HELVETICA, 12f)
    contentStream.newLineAtOffset(margin, yPosition)
    contentStream.showText(("Date: ${formatTransferDate(transfer.transferDate)}").replaceRomanianChars())
    contentStream.endText()
    yPosition -= 40f

    // Draw table header background
    contentStream.setNonStrokingColor(220, 220, 220)
    contentStream.addRect(tableMargin, yPosition - headerHeight, tableWidth, headerHeight)
    contentStream.fill()
    contentStream.setNonStrokingColor(0, 0, 0)

    // Draw table header text
    contentStream.setFont(PDType1Font.HELVETICA_BOLD, 10f)
    var xPosition = tableMargin
    for ((i, header) in headers.withIndex()) {
        contentStream.beginText()
        contentStream.newLineAtOffset(xPosition + cellMargin, yPosition - headerHeight + (headerHeight - 10f) / 2f)
        contentStream.showText(header.replaceRomanianChars())
        contentStream.endText()
        xPosition += cellWidths[i]
    }

    // Draw header lines
    contentStream.setLineWidth(0.5f)
    contentStream.moveTo(tableMargin, yPosition - headerHeight)
    contentStream.lineTo(tableMargin + tableWidth, yPosition - headerHeight)
    contentStream.stroke()
    xPosition = tableMargin
    for (width in cellWidths) {
        contentStream.moveTo(xPosition, yPosition)
        contentStream.lineTo(xPosition, yPosition - headerHeight)
        contentStream.stroke()
        xPosition += width
    }
    contentStream.moveTo(tableMargin + tableWidth, yPosition)
    contentStream.lineTo(tableMargin + tableWidth, yPosition - headerHeight)
    contentStream.stroke()

    yPosition -= headerHeight

    // Draw table content
    contentStream.setFont(PDType1Font.HELVETICA, 10f)
    for (animal in animals) {
        xPosition = tableMargin
        val rowData = listOf(animal.id, animal.species, animal.gender, animal.birthDate)
        for ((i, data) in rowData.withIndex()) {
            contentStream.beginText()
            contentStream.newLineAtOffset(xPosition + cellMargin, yPosition + (rowHeight - 10f) / 2f)
            contentStream.showText(data.replaceRomanianChars())
            contentStream.endText()
            xPosition += cellWidths[i]
        }
        // Draw horizontal line below row
        contentStream.setLineWidth(0.5f)
        contentStream.moveTo(tableMargin, yPosition)
        contentStream.lineTo(tableMargin + tableWidth, yPosition)
        contentStream.stroke()
        // Draw vertical lines for this row
        xPosition = tableMargin
        for (width in cellWidths) {
            contentStream.moveTo(xPosition, yPosition)
            contentStream.lineTo(xPosition, yPosition - rowHeight)
            contentStream.stroke()
            xPosition += width
        }
        contentStream.moveTo(tableMargin + tableWidth, yPosition)
        contentStream.lineTo(tableMargin + tableWidth, yPosition - rowHeight)
        contentStream.stroke()
        yPosition -= rowHeight
    }

    contentStream.close()
    document.save(outputStream)
    document.close()
} 