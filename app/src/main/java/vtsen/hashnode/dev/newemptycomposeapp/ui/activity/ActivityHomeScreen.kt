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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.kpi.BillingPeriodStore

/**
 * DatePicker/DatePickerDialog son experimentales en Material3, por eso el OptIn. [1](https://developer.android.com/develop/ui/compose/components/datepickers)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityHomeScreen(
    viewModel: ActivityViewModel,
    onNewTravelClick: () -> Unit,
    onCurrentTravelClick: () -> Unit
) {
    val context = LocalContext.current
    val zone = remember { ZoneId.systemDefault() }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isExporting by remember { mutableStateOf(false) }

    val allTravels by viewModel.allTravels.collectAsStateWithLifecycle()
    val currentTravel by viewModel.currentTravel.collectAsStateWithLifecycle()

    // ====== Periodo de facturación (DataStore) ======
    val storedPeriod by BillingPeriodStore.periodFlow(context).collectAsStateWithLifecycle(
        initialValue = vtsen.hashnode.dev.newemptycomposeapp.ui.activity.kpi.BillingPeriod(0L, 0L)
    )

    // Default: 1º a último del mes (si no hay valores guardados)
    val defaultPeriod = remember { currentMonthRangeMillis(zone) }

    // Estado local visible en UI
    var fromMillis by remember { mutableStateOf(0L) }
    var toMillis by remember { mutableStateOf(0L) }

    LaunchedEffect(storedPeriod.fromMillis, storedPeriod.toMillis) {
        if (storedPeriod.fromMillis == 0L || storedPeriod.toMillis == 0L) {
            // Guardamos el default de mes actual la primera vez
            BillingPeriodStore.savePeriod(context, defaultPeriod.first, defaultPeriod.second)
            fromMillis = defaultPeriod.first
            toMillis = defaultPeriod.second
        } else {
            fromMillis = storedPeriod.fromMillis
            toMillis = storedPeriod.toMillis
        }
    }

    // Dialogs DatePicker (Desde / Hasta)
    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }

    val fromDate = remember(fromMillis) {
        if (fromMillis == 0L) LocalDate.now()
        else BillingPeriodStore.millisToLocalDate(fromMillis, zone)
    }
    val toDate = remember(toMillis) {
        if (toMillis == 0L) LocalDate.now()
        else BillingPeriodStore.millisToLocalDate(toMillis, zone)
    }

    val fromPickerState = rememberDatePickerState(
        initialSelectedDateMillis = BillingPeriodStore.localDateStartMillis(fromDate, zone)
    )
    val toPickerState = rememberDatePickerState(
        initialSelectedDateMillis = BillingPeriodStore.localDateStartMillis(toDate, zone)
    )

    fun saveRange(newFrom: LocalDate, newTo: LocalDate) {
        val f = BillingPeriodStore.localDateStartMillis(newFrom, zone)
        val t = BillingPeriodStore.localDateEndMillis(newTo, zone)
        coroutineScope.launch {
            BillingPeriodStore.savePeriod(context, f, t)
        }
    }

    // ====== Filtrado por periodo: SOLO viajes del rango ======
    val periodTravels by remember(allTravels, fromMillis, toMillis) {
        derivedStateOf {
            if (fromMillis == 0L || toMillis == 0L) emptyList()
            else allTravels.filter { it.startTimestamp in fromMillis..toMillis }
        }
    }

    // KPIs actuales (los tuyos los redefiniremos después; aquí solo cambiamos el filtro)
    val periodBillingTotal by remember(periodTravels) { derivedStateOf { periodTravels.sumOf { it.billingExpected } } }
    val periodTripsTotal by remember(periodTravels) { derivedStateOf { periodTravels.size } }
    val periodClosedImputedHours by remember(periodTravels) {
        derivedStateOf { periodTravels.filter { it.status == TravelStatus.CLOSED }.sumOf { it.hoursImputed ?: 0.0 } }
    }
    val draftInProgress by remember(currentTravel) { derivedStateOf { currentTravel?.hoursDraft } }
    val periodModifiedCount by remember(periodTravels) {
        derivedStateOf { periodTravels.count { it.status == TravelStatus.CLOSED && it.hoursModified } }
    }

    fun triggerExport(folderUri: Uri) {
        if (isExporting) return
        isExporting = true

        val fileName = ExportUtils.currentMonthFileName()
        coroutineScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    exportMonthlyCsvIO(context, folderUri, periodTravels)
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

    // ====== UI ======
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewTravelClick,
                containerColor = MaterialTheme.colorScheme.primary
            ) { Text("+", style = MaterialTheme.typography.titleLarge) }
        }
    ) { padding ->

        // Dialog "Desde"
        if (showFromPicker) {
            DatePickerDialog(
                onDismissRequest = { showFromPicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val pickedMillis = fromPickerState.selectedDateMillis
                        if (pickedMillis != null) {
                            val pickedDate = BillingPeriodStore.millisToLocalDate(pickedMillis, zone)
                            // Si el usuario elige desde > hasta, ajustamos "hasta" al mismo día
                            val newTo = if (pickedDate.isAfter(toDate)) pickedDate else toDate
                            saveRange(pickedDate, newTo)
                        }
                        showFromPicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showFromPicker = false }) { Text("Cancelar") }
                }
            ) { DatePicker(state = fromPickerState) }
        }

        // Dialog "Hasta"
        if (showToPicker) {
            DatePickerDialog(
                onDismissRequest = { showToPicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val pickedMillis = toPickerState.selectedDateMillis
                        if (pickedMillis != null) {
                            val pickedDate = BillingPeriodStore.millisToLocalDate(pickedMillis, zone)
                            // Si el usuario elige hasta < desde, ajustamos "desde" al mismo día
                            val newFrom = if (pickedDate.isBefore(fromDate)) pickedDate else fromDate
                            saveRange(newFrom, pickedDate)
                        }
                        showToPicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showToPicker = false }) { Text("Cancelar") }
                }
            ) { DatePicker(state = toPickerState) }
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // PANEL IZQUIERDO 35%: Periodo + KPIs + Export
            Column(
                modifier = Modifier.weight(0.35f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Resumen", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

                // ====== Periodo de facturación ======
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Periodo de facturación", fontWeight = FontWeight.Bold)

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { showFromPicker = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Desde: ${fromDate.format(dateFormatter)}")
                            }
                            Button(
                                onClick = { showToPicker = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Hasta: ${toDate.format(dateFormatter)}")
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                    val (mStart, mEnd) = currentMonthRangeMillis(zone)
                                    coroutineScope.launch { BillingPeriodStore.savePeriod(context, mStart, mEnd) }
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Este mes") }

                            Button(
                                onClick = {
                                    // Reset = default mes actual
                                    coroutineScope.launch { BillingPeriodStore.savePeriod(context, defaultPeriod.first, defaultPeriod.second) }
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Reset") }
                        }
                    }
                }

                // Separador visual
                Spacer(modifier = Modifier.height(4.dp))

                // KPIs (por ahora los básicos; los redefinimos en el siguiente paso)
                KpiCard("Facturación periodo", formatCurrency(periodBillingTotal), "Estimado", MaterialTheme.colorScheme.primaryContainer)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    KpiCard("Viajes", periodTripsTotal.toString(), "En periodo", MaterialTheme.colorScheme.surfaceVariant, Modifier.weight(1f))
                    KpiCard("Modificados", "$periodModifiedCount ⚠️", "Imputadas ≠ calc.", MaterialTheme.colorScheme.errorContainer, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    KpiCard("Horas cerradas", formatHours(periodClosedImputedHours), "Imputadas", MaterialTheme.colorScheme.surfaceVariant, Modifier.weight(1f))
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

            // PANEL DERECHO 65%: listado SOLO del periodo
            Column(
                modifier = Modifier.weight(0.65f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Viajes del periodo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // EN CURSO solo si cae dentro del periodo
                    val showCurrent = currentTravel?.startTimestamp?.let { it in fromMillis..toMillis } == true
                    if (showCurrent) {
                        item {
                            val t = currentTravel!!
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onCurrentTravelClick() }
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text("🟢 EN CURSO (tocar para continuar)", fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("${t.origin} → ${t.destination}", style = MaterialTheme.typography.titleMedium)
                                    Text("KM inicio: ${t.kmStart} | Draft: ${t.hoursDraft ?: 0.0}h")
                                }
                            }
                        }
                    }

                    // Lista de cerrados del periodo
                    val closed = periodTravels.filter { it.status == TravelStatus.CLOSED }
                    items(closed) { t -> TravelRowCard(t, zone, dateFormatter) }
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
private fun TravelRowCard(
    travel: TravelEntity,
    zone: ZoneId,
    dateFormatter: DateTimeFormatter
) {
    val warning = if (travel.hoursModified) " ⚠️" else ""
    val date = BillingPeriodStore.millisToLocalDate(travel.startTimestamp, zone).format(dateFormatter)

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("$date · ${travel.origin} → ${travel.destination}$warning", fontWeight = FontWeight.SemiBold)
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

private fun currentMonthRangeMillis(zone: ZoneId): Pair<Long, Long> {
    val now = LocalDate.now()
    val start = now.with(TemporalAdjusters.firstDayOfMonth())
    val end = now.with(TemporalAdjusters.lastDayOfMonth())
    val startMillis = start.atStartOfDay(zone).toInstant().toEpochMilli()
    val endMillis = end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
    return startMillis to endMillis
}

private fun formatCurrency(value: Double): String = "${formatCurrencyNumber(value)} €"
private fun formatCurrencyNumber(value: Double): String = String.format(Locale.getDefault(), "%.2f", value)
private fun formatHours(value: Double): String = String.format(Locale.getDefault(), "%.2f h", value)
