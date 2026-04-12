package vtsen.hashnode.dev.newemptycomposeapp.ui.activity

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelEntity
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelStatus
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.export.ExportPreferences
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.export.ExportUtils
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.export.TravelCsvExporter

@Composable
fun ActivityHomeScreen(
    viewModel: ActivityViewModel,
    onNewTravelClick: () -> Unit,
    onCurrentTravelClick: () -> Unit
) {
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isExporting by remember { mutableStateOf(false) }

    val allTravels by viewModel.allTravels.collectAsStateWithLifecycle()
    val currentTravel by viewModel.currentTravel.collectAsStateWithLifecycle()

    val (monthStart, monthEnd) = remember { currentMonthRangeMillis() }

    val monthTravels by remember(allTravels, monthStart, monthEnd) {
        derivedStateOf { allTravels.filter { it.startTimestamp in monthStart..monthEnd } }
    }
    val monthTravelsLatest by rememberUpdatedState(monthTravels)

    val monthBillingTotal by remember(monthTravels) { derivedStateOf { monthTravels.sumOf { it.billingExpected } } }
    val monthTripsTotal by remember(monthTravels) { derivedStateOf { monthTravels.size } }
    val monthClosedImputedHours by remember(monthTravels) {
        derivedStateOf { monthTravels.filter { it.status == TravelStatus.CLOSED }.sumOf { it.hoursImputed ?: 0.0 } }
    }
    val draftInProgress by remember(currentTravel) { derivedStateOf { currentTravel?.hoursDraft } }
    val monthModifiedCount by remember(monthTravels) {
        derivedStateOf { monthTravels.count { it.status == TravelStatus.CLOSED && it.hoursModified } }
    }

    fun triggerExport(folderUri: Uri) {
        if (isExporting) return
        isExporting = true

        val fileName = ExportUtils.currentMonthFileName()

        coroutineScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    exportMonthlyCsvIO(context, folderUri, monthTravelsLatest)
                }
                snackbarHostState.showSnackbar("✅ Exportado: $fileName")
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("❌ Error al exportar: ${e.localizedMessage ?: "desconocido"}")
            } finally {
                isExporting = false
            }
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            ExportPreferences.saveFolderUri(context, it)
            triggerExport(it)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewTravelClick,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }
        }
    ) { padding ->

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // PANEL IZQUIERDO 35%: KPIs + Export
            Column(
                modifier = Modifier.weight(0.35f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Resumen mensual", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

                KpiCard("Facturación mes (total)", formatCurrency(monthBillingTotal), "Cerrados + en curso", MaterialTheme.colorScheme.primaryContainer)

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    KpiCard("Viajes del mes", monthTripsTotal.toString(), "Totales", MaterialTheme.colorScheme.surfaceVariant, Modifier.weight(1f))
                    KpiCard("Modificados", "$monthModifiedCount ⚠️", "Imputadas ≠ calc.", MaterialTheme.colorScheme.errorContainer, Modifier.weight(1f))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    KpiCard("Horas cerradas", formatHours(monthClosedImputedHours), "Imputadas", MaterialTheme.colorScheme.surfaceVariant, Modifier.weight(1f))
                    KpiCard("Horas draft", draftInProgress?.let { formatHours(it) } ?: "—", "En curso", MaterialTheme.colorScheme.secondaryContainer, Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        val folderUri = ExportPreferences.getFolderUri(context)
                        if (folderUri == null) folderPickerLauncher.launch(null) else triggerExport(folderUri)
                    },
                    enabled = !isExporting,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    if (isExporting) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator(strokeWidth = 2.dp)
                            Text("EXPORTANDO…", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text("📤 EXPORTAR CSV A DRIVE", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // PANEL DERECHO 65%: Timeline
            Column(
                modifier = Modifier.weight(0.65f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Línea de tiempo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    currentTravel?.let { t ->
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onCurrentTravelClick() }
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text("🟢 EN CURSO (tocar para continuar)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("${t.origin} → ${t.destination}", style = MaterialTheme.typography.titleMedium)
                                    Text("KM inicio: ${t.kmStart} | Draft: ${t.hoursDraft ?: 0.0}h")
                                }
                            }
                        }
                    }

                    val closedMonthTravels = monthTravels.filter { it.status == TravelStatus.CLOSED }
                    items(closedMonthTravels) { t ->
                        TravelRowCard(t)
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TravelRowCard(travel: TravelEntity) {
    val warning = if (travel.hoursModified) " ⚠️" else ""
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${travel.origin} → ${travel.destination}$warning", fontWeight = FontWeight.SemiBold)
                Text("€ ${formatCurrencyNumber(travel.billingExpected)}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Text("Horas imputadas: ${travel.hoursImputed?.let { formatHours(it) } ?: "—"}")
            Text("Ref: ${travel.description}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun exportMonthlyCsvIO(context: Context, folderUri: Uri, travels: List<TravelEntity>) {
    val resolver = context.contentResolver
    val fileName = ExportUtils.currentMonthFileName()

    resolver.query(
        DocumentsContract.buildChildDocumentsUriUsingTree(folderUri, DocumentsContract.getTreeDocumentId(folderUri)),
        arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
        null, null, null
    )?.use { cursor ->
        while (cursor.moveToNext()) {
            val documentId = cursor.getString(0)
            val displayName = cursor.getString(1)
            if (displayName == fileName) {
                val fileUri = DocumentsContract.buildDocumentUriUsingTree(folderUri, documentId)
                DocumentsContract.deleteDocument(resolver, fileUri)
                break
            }
        }
    }

    val newFileUri = DocumentsContract.createDocument(resolver, folderUri, "text/csv", fileName)
        ?: throw IllegalStateException("No se pudo crear el archivo en Drive")

    resolver.openOutputStream(newFileUri)?.use { stream ->
        TravelCsvExporter.writeCsv(stream, travels)
    } ?: throw IllegalStateException("No se pudo abrir OutputStream del documento")
}

private fun currentMonthRangeMillis(): Pair<Long, Long> {
    val now = LocalDate.now()
    val start = now.with(TemporalAdjusters.firstDayOfMonth())
    val end = now.with(TemporalAdjusters.lastDayOfMonth())

    val zone = ZoneId.systemDefault()
    val startMillis = start.atStartOfDay(zone).toInstant().toEpochMilli()
    val endMillis = end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
    return startMillis to endMillis
}

private fun formatCurrency(value: Double): String = "${formatCurrencyNumber(value)} €"
private fun formatCurrencyNumber(value: Double): String = String.format(Locale.getDefault(), "%.2f", value)
private fun formatHours(value: Double): String = String.format(Locale.getDefault(), "%.2f h", value)
