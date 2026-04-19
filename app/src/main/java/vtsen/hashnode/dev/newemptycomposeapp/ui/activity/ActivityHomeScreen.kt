package vtsen.hashnode.dev.newemptycomposeapp.ui.activity

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.launch
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelEntity
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelStatus
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.export.AxisImportCoordinator
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.export.ExportPreferences
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.kpi.BillingPeriodStore
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.kpi.CalendarManagementDialog
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.kpi.CalendarOverridesStore
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.kpi.CalendarOverridesStore.Holiday
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.kpi.CalendarOverridesStore.Vacation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityHomeScreen(
    viewModel: ActivityViewModel,
    onNewTravelClick: () -> Unit,
    onCurrentTravelClick: () -> Unit,
    onEditTravelClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val zone = remember { ZoneId.systemDefault() }
    val localeEs = remember { Locale("es", "ES") }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // =========================
    // Estado desde ViewModel
    // =========================
    val allTravels by viewModel.allTravels.collectAsStateWithLifecycle(initialValue = emptyList())
    val currentTravel by viewModel.currentTravel.collectAsStateWithLifecycle(initialValue = null)
    val stopsInPeriod by viewModel.stopsInPeriod.collectAsStateWithLifecycle(initialValue = emptyList())

    val periodDates by viewModel.periodDates.collectAsStateWithLifecycle(
        initialValue = LocalDate.now(zone) to LocalDate.now(zone)
    )
    val fromDate = periodDates.first
    val toDate = periodDates.second

    val isExporting by viewModel.isExporting.collectAsStateWithLifecycle(initialValue = false)
    val exportError by viewModel.exportError.collectAsStateWithLifecycle(initialValue = null)

    val isImporting by viewModel.isImporting.collectAsStateWithLifecycle(initialValue = false)
    val importError by viewModel.importError.collectAsStateWithLifecycle(initialValue = null)
    val lastImportResult by viewModel.lastImportResult.collectAsStateWithLifecycle(initialValue = null)

    // Estado local para la URI del archivo maestro (Cache visual)
    var masterFileUri by remember { mutableStateOf(ExportPreferences.getMasterFileUri(context)) }

    // =========================
    // Pickers de Fecha (Periodo)
    // =========================
    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }

    val fromPickerState = rememberDatePickerState(
        initialSelectedDateMillis = BillingPeriodStore.localDateStartMillis(fromDate, zone)
    )
    val toPickerState = rememberDatePickerState(
        initialSelectedDateMillis = BillingPeriodStore.localDateStartMillis(toDate, zone)
    )

    // =========================
    // Feedback Snackbars
    // =========================
    LaunchedEffect(exportError) {
        exportError?.let {
            snackbarHostState.showSnackbar("❌ Export: $it")
            viewModel.clearExportError()
        }
    }

    LaunchedEffect(importError) {
        importError?.let {
            snackbarHostState.showSnackbar("❌ Import: $it")
            viewModel.clearImportError()
        }
    }

    LaunchedEffect(lastImportResult) {
        lastImportResult?.let { r ->
            snackbarHostState.showSnackbar(
                "✅ Import OK: ${r.travelsImported} viajes, ${r.stopsImported} paradas."
            )
        }
    }

    // =========================
    // Calendar Overrides
    // =========================
    val allHolidays by CalendarOverridesStore.holidaysFlow(context).collectAsStateWithLifecycle(initialValue = emptyList())
    val allVacations by CalendarOverridesStore.vacationsFlow(context).collectAsStateWithLifecycle(initialValue = emptyList())

    var showCalendarManager by remember { mutableStateOf(false) }
    var showAddHoliday by remember { mutableStateOf(false) }
    var showAddVacation by remember { mutableStateOf(false) }
    var holidayDesc by remember { mutableStateOf("") }
    var vacationDesc by remember { mutableStateOf("") }

    val holidaysInPeriod by remember(allHolidays, fromDate, toDate) {
        derivedStateOf { allHolidays.filter { it.date in fromDate..toDate } }
    }
    val holidayByDateInPeriod by remember(holidaysInPeriod) {
        derivedStateOf { holidaysInPeriod.associateBy { it.date } }
    }
    val vacationsInPeriod by remember(allVacations, fromDate, toDate) {
        derivedStateOf { allVacations.filter { v -> !(v.to.isBefore(fromDate) || v.from.isAfter(toDate)) } }
    }
    val vacationDatesInPeriod by remember(vacationsInPeriod, fromDate, toDate) {
        derivedStateOf { expandVacationDates(vacationsInPeriod, fromDate, toDate) }
    }
    val vacationDescsByDateInPeriod by remember(vacationsInPeriod, fromDate, toDate) {
        derivedStateOf { buildVacationDescriptionsByDate(vacationsInPeriod, fromDate, toDate) }
    }

    // =========================
    // Lógica de Timeline
    // =========================
    val fromMillis = remember(fromDate, zone) { BillingPeriodStore.localDateStartMillis(fromDate, zone) }
    val toMillis = remember(toDate, zone) { BillingPeriodStore.localDateEndMillis(toDate, zone) }

    LaunchedEffect(fromMillis, toMillis) {
        if (fromMillis > 0L && toMillis > 0L) viewModel.setStopsRange(fromMillis, toMillis)
    }

    val periodTravels by remember(allTravels, fromMillis, toMillis) {
        derivedStateOf { allTravels.filter { it.startTimestamp in fromMillis..toMillis } }
    }

    val daysInRange = remember(fromDate, toDate) { datesBetweenInclusive(fromDate, toDate) }

    val travelsByDay = remember(periodTravels, zone) {
        periodTravels.groupBy { t -> BillingPeriodStore.millisToLocalDateOrToday(t.startTimestamp, zone) }
    }

    val stopsCountByDay = remember(stopsInPeriod, zone) {
        stopsInPeriod.groupBy { s -> BillingPeriodStore.millisToLocalDateOrToday(s.timestamp, zone) }
            .mapValues { it.value.size }
    }

    // =========================
    // Cálculos KPIs
    // =========================
    val totalEstimadoPeriodo by remember(periodTravels) { derivedStateOf { periodTravels.sumOf { it.billingExpected } } }
    val horasImputadasPeriodo by remember(periodTravels) {
        derivedStateOf { periodTravels.filter { it.status == TravelStatus.CLOSED }.sumOf { it.hoursImputed ?: 0.0 } }
    }
    val kmPeriodo by remember(periodTravels) {
        derivedStateOf {
            periodTravels.filter { it.status == TravelStatus.CLOSED }
                .sumOf { t -> ((t.kmEnd ?: t.kmStart) - t.kmStart).coerceAtLeast(0) }
        }
    }
    val diasLaborablesReales by remember(fromDate, toDate, vacationDatesInPeriod, holidaysInPeriod) {
        derivedStateOf {
            countWeekdaysInclusive(fromDate, toDate) -
                countWeekdaysInSet(fromDate, toDate, holidaysInPeriod.map { it.date }.toSet()) -
                countWeekdaysInSet(fromDate, toDate, vacationDatesInPeriod)
        }
    }
    val diasLaborables = diasLaborablesReales.coerceAtLeast(0)
    val totalFacturarObjetivo by remember(diasLaborables) { derivedStateOf { 350.0 * diasLaborables } }
    val horasObjetivo by remember(diasLaborables) { derivedStateOf { 8.0 * diasLaborables } }
    val deltaHoras by remember(horasObjetivo, horasImputadasPeriodo) { derivedStateOf { horasObjetivo - horasImputadasPeriodo } }

    val totalAnualEstimado by remember(allTravels) {
        derivedStateOf {
            val year = LocalDate.now().year
            allTravels.filter { BillingPeriodStore.millisToLocalDateOrToday(it.startTimestamp, zone).year == year }
                .sumOf { it.billingExpected }
        }
    }
    val pendienteFacturar by remember(allTravels) {
        derivedStateOf { allTravels.filter { !it.isInvoiced }.sumOf { it.billingExpected } }
    }

    // =========================
    // SAF: Launchers Drive
    // =========================

    // Launcher para CREAR o ELEGIR el archivo maestro
    val createMasterLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
        ExportPreferences.saveMasterFileUri(context, uri)
        masterFileUri = uri
        viewModel.exportToMasterFileUri(uri)
        coroutineScope.launch { snackbarHostState.showSnackbar("✅ Archivo maestro vinculado.") }
    }

    // Launcher para IMPORTAR CSV
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var showImportConfirm by remember { mutableStateOf(false) }

    val importCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        pendingImportUri = uri
        showImportConfirm = true
    }

    // =========================
    // Diálogos de Confirmación / Edición
    // =========================

    // Confirmar Importación
    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = { showImportConfirm = false },
            title = { Text("¿Importar base de datos?", fontWeight = FontWeight.Bold) },
            text = { Text("⚠️ Esta acción BORRARÁ todos los datos actuales de la tablet y los sustituirá por los del CSV. No se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    val uri = pendingImportUri
                    showImportConfirm = false
                    pendingImportUri = null
                    if (uri != null) viewModel.importFromDriveCsv(uri, AxisImportCoordinator.ImportStrategy.REPLACE_ALL)
                }) { Text("IMPORTAR Y BORRAR ACTUAL", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showImportConfirm = false; pendingImportUri = null }) { Text("Cancelar") } }
        )
    }

    // Diálogos de Edición/Borrado de Viaje (Estado)
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var travelToDeleteId by remember { mutableStateOf<String?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingTravelId by remember { mutableStateOf<String?>(null) }

    var editOrigin by remember { mutableStateOf("") }
    var editDestination by remember { mutableStateOf("") }
    var editDescription by remember { mutableStateOf("") }
    var editBilling by remember { mutableStateOf("") }
    var editHasDiet by remember { mutableStateOf(false) }
    var editKmStart by remember { mutableStateOf("") }
    var editKmEnd by remember { mutableStateOf("") }
    var editHoursImputed by remember { mutableStateOf("") }
    var editIsInvoiced by remember { mutableStateOf(false) }

    fun openEditDialog(travel: TravelEntity) {
        editingTravelId = travel.id
        editOrigin = travel.origin
        editDestination = travel.destination
        editDescription = travel.description
        editBilling = String.format(Locale.US, "%.2f", travel.billingExpected)
        editHasDiet = travel.hasDiet
        editKmStart = travel.kmStart.toString()
        editKmEnd = travel.kmEnd?.toString() ?: ""
        editHoursImputed = travel.hoursImputed?.toString() ?: ""
        editIsInvoiced = travel.isInvoiced
        showEditDialog = true
    }

    if (showEditDialog && editingTravelId != null) {
        val original = allTravels.firstOrNull { it.id == editingTravelId }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Editar viaje", fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(editOrigin, { editOrigin = it }, label = { Text("Origen") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(editDestination, { editDestination = it }, label = { Text("Destino") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(editDescription, { editDescription = it }, label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(editBilling, { editBilling = it }, label = { Text("Facturación (€)") }, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Dieta", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        Switch(editHasDiet, { editHasDiet = it })
                    }
                    OutlinedTextField(editKmStart, { editKmStart = it.filter(Char::isDigit) }, label = { Text("KM inicio") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(editKmEnd, { editKmEnd = it.filter(Char::isDigit) }, label = { Text("KM fin") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(editHoursImputed, { editHoursImputed = it }, label = { Text("Horas imputadas") }, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Facturado", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        Switch(editIsInvoiced, { editIsInvoiced = it })
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val base = original ?: return@TextButton
                    val updated = base.copy(
                        origin = editOrigin.trim(), destination = editDestination.trim(), description = editDescription.trim(),
                        billingExpected = editBilling.replace(',', '.').toDoubleOrNull() ?: 0.0,
                        hasDiet = editHasDiet, kmStart = editKmStart.toIntOrNull() ?: 0, kmEnd = editKmEnd.toIntOrNull(),
                        hoursImputed = editHoursImputed.replace(',', '.').toDoubleOrNull(), isInvoiced = editIsInvoiced
                    )
                    viewModel.updateTravel(updated)
                    showEditDialog = false
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showEditDialog = false }) { Text("Cancelar") } }
        )
    }

    if (showDeleteConfirm && travelToDeleteId != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Eliminar viaje") },
            text = { Text("⚠️ ¿Seguro que quieres borrar este viaje y todas sus paradas?") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteTravel(travelToDeleteId!!); showDeleteConfirm = false }) {
                    Text("BORRAR", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") } }
        )
    }

    // =========================
    // Scaffold UI
    // =========================
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("AXIS · Activity") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                actions = {
                    TextButton(onClick = { showCalendarManager = true }) { Text("📅", fontSize = MaterialTheme.typography.titleLarge.fontSize) }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewTravelClick, containerColor = MaterialTheme.colorScheme.primary) {
                Text("+", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { padding ->

        // UI Pickers de Fecha
        if (showFromPicker) {
            DatePickerDialog(
                onDismissRequest = { showFromPicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        fromPickerState.selectedDateMillis?.let { viewModel.setBillingPeriodDates(BillingPeriodStore.millisToLocalDateOrToday(it, zone), toDate) }
                        showFromPicker = false
                    }) { Text("OK") }
                },
                dismissButton = { TextButton(onClick = { showFromPicker = false }) { Text("Cancelar") } }
            ) { DatePicker(state = fromPickerState) }
        }

        if (showToPicker) {
            DatePickerDialog(
                onDismissRequest = { showToPicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        toPickerState.selectedDateMillis?.let { viewModel.setBillingPeriodDates(fromDate, BillingPeriodStore.millisToLocalDateOrToday(it, zone)) }
                        showToPicker = false
                    }) { Text("OK") }
                },
                dismissButton = { TextButton(onClick = { showToPicker = false }) { Text("Cancelar") } }
            ) { DatePicker(state = toPickerState) }
        }

        Row(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            
            // IZQ: Resumen y Acciones
            Column(modifier = Modifier.weight(0.35f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Resumen", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { showFromPicker = true }, modifier = Modifier.weight(1f)) { Text("D: ${fromDate.format(dateFormatter)}") }
                            Button(onClick = { showToPicker = true }, modifier = Modifier.weight(1f)) { Text("H: ${toDate.format(dateFormatter)}") }
                        }
                        Button(onClick = { viewModel.resetBillingToThisMonth() }, modifier = Modifier.fillMaxWidth()) { Text("Este mes / Reset") }
                    }
                }

                SectionDivider()
                KpiLine("Objetivo Fact.", formatCurrency(totalFacturarObjetivo), "350€/día laborable")
                KpiLine("Horas Real", formatHours(horasImputadasPeriodo), "Solo cerrados")
                KpiLine("KM Periodo", "$kmPeriodo km", "Total cerrados")

                SectionDivider()
                KpiLine("Total Anual", formatCurrency(totalAnualEstimado), "Año actual")
                KpiLine("Pendiente", formatCurrency(pendienteFacturar), "No facturados")

                Spacer(modifier = Modifier.height(16.dp))

                // EXPORTAR (Samsung Ready)
                Button(
                    onClick = {
                        val uri = masterFileUri
                        if (uri == null) createMasterLauncher.launch("AXIS_Master_Database.csv")
                        else viewModel.exportToMasterFileUri(uri)
                    },
                    enabled = !isExporting && !isImporting,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    if (isExporting) CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = 8.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Text(if (masterFileUri == null) "☁️ CONFIGURAR DRIVE" else "📤 EXPORTAR A DRIVE")
                }

                TextButton(onClick = { createMasterLauncher.launch("AXIS_Master_Database.csv") }, modifier = Modifier.fillMaxWidth()) {
                    Text("⚙️ Cambiar archivo de Drive")
                }

                // IMPORTAR
                Button(
                    onClick = { importCsvLauncher.launch(arrayOf("text/csv", "text/*")) },
                    enabled = !isExporting && !isImporting,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    if (isImporting) CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = 8.dp), strokeWidth = 2.dp)
                    Text("📥 IMPORTAR CSV")
                }
            }

            // DER: Timeline
            Column(modifier = Modifier.weight(0.65f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Timeline del periodo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                    items(daysInRange, key = { it.toEpochDay() }) { day ->
                        val isWeekend = day.dayOfWeek == DayOfWeek.SATURDAY || day.dayOfWeek == DayOfWeek.SUNDAY
                        val holiday = holidayByDateInPeriod[day]
                        val vacations = vacationDescsByDateInPeriod[day].orEmpty()
                        
                        DayHeader(day, dateFormatter, localeEs, isWeekend, holiday != null, vacations.isNotEmpty())

                        // Viaje en curso
                        val currentDay = currentTravel?.startTimestamp?.let { BillingPeriodStore.millisToLocalDateOrToday(it, zone) }
                        if (currentTravel != null && currentTravel!!.status == TravelStatus.IN_PROGRESS && currentDay == day) {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth().clickable { onCurrentTravelClick() }) {
                                Column(Modifier.padding(16.dp)) {
                                    Text("🟢 EN CURSO", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                    Text("${currentTravel!!.origin} → ${currentTravel!!.destination}", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }

                        if (holiday != null) {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)), modifier = Modifier.fillMaxWidth()) {
                                Text("🎉 Festivo: ${holiday.description}", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.SemiBold)
                            }
                        }

                        vacations.forEach { v ->
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer), modifier = Modifier.fillMaxWidth()) {
                                Text("🏖️ Vacaciones: $v", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.SemiBold)
                            }
                        }

                        val stopsToday = stopsCountByDay[day] ?: 0
                        if (stopsToday > 0) {
                            Text("📍 $stopsToday paradas registradas", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp))
                        }

                        val travelsToday = (travelsByDay[day] ?: emptyList()).filter { it.status == TravelStatus.CLOSED }.sortedBy { it.startTimestamp }
                        travelsToday.forEach { tr ->
                            TravelRowCard(tr, zone, dateFormatter, { viewModel.setFacturado(tr.id, it) }, { openEditDialog(tr) }, { travelToDeleteId = tr.id; showDeleteConfirm = true })
                        }
                    }
                }
            }
        }
    }

    // Diálogos de Calendario Manager (Al final para evitar solapamientos)
    if (showCalendarManager) {
        CalendarManagementDialog(
            holidays = allHolidays, vacations = allVacations,
            onAddHoliday = { showAddHoliday = true }, onAddVacation = { showAddVacation = true },
            onDeleteHoliday = { h -> coroutineScope.launch { CalendarOverridesStore.removeHolidayRaw(context, CalendarOverridesStore.toRawHoliday(h)) } },
            onDeleteVacation = { v -> coroutineScope.launch { CalendarOverridesStore.removeVacationRaw(context, CalendarOverridesStore.toRawVacation(v)) } },
            onDismiss = { showCalendarManager = false }
        )
    }
}

// =========================
// Helpers UI
// =========================

@Composable
private fun BlockTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
}

@Composable
private fun KpiLine(title: String, value: String, subtitle: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Column(Modifier.padding(10.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TravelRowCard(t: TravelEntity, zone: ZoneId, dateFormatter: DateTimeFormatter, onToggle: (Boolean) -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val warning = if (t.hoursModified) " ⚠️" else ""
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth().clickable { onEdit() }) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${t.origin} → ${t.destination}$warning", fontWeight = FontWeight.SemiBold)
                    if (t.description.isNotBlank()) Text("Ref: ${t.description}", style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = onEdit) { Text("✏️") }
                TextButton(onClick = onDelete) { Text("🗑️") }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Horas: ${String.format("%.1f", t.hoursImputed ?: 0.0)} h", modifier = Modifier.weight(1f))
                Text("Fact.", style = MaterialTheme.typography.labelSmall)
                Checkbox(t.isInvoiced, onToggle, colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary))
            }
            Text("${String.format("%.2f", t.billingExpected)} €", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun DayHeader(day: LocalDate, dateFormatter: DateTimeFormatter, localeEs: Locale, isWeekend: Boolean, isHoliday: Boolean, isVacation: Boolean) {
    val weekdayName = day.dayOfWeek.getDisplayName(TextStyle.FULL, localeEs).replaceFirstChar { it.titlecase() }
    val containerColor = when {
        isHoliday || isWeekend -> MaterialTheme.colorScheme.errorContainer
        isVacation -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val contentColor = when {
        isHoliday || isWeekend -> MaterialTheme.colorScheme.error
        isVacation -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    Card(colors = CardDefaults.cardColors(containerColor = containerColor), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp)) {
            Text("$weekdayName ${day.dayOfMonth}", fontWeight = FontWeight.Bold, color = contentColor)
            Text(day.format(dateFormatter), style = MaterialTheme.typography.bodySmall, color = contentColor.copy(alpha = 0.8f))
        }
    }
}

// --- Lógica de Apoyo ---
private fun datesBetweenInclusive(from: LocalDate, to: LocalDate): List<LocalDate> {
    if (to.isBefore(from)) return emptyList()
    val out = ArrayList<LocalDate>()
    var d = from
    while (!d.isAfter(to)) { out.add(d); d = d.plusDays(1) }
    return out
}

private fun countWeekdaysInclusive(from: LocalDate, to: LocalDate): Int {
    var count = 0; var d = from
    while (!d.isAfter(to)) { if (d.dayOfWeek != DayOfWeek.SATURDAY && d.dayOfWeek != DayOfWeek.SUNDAY) count++; d = d.plusDays(1) }
    return count
}

private fun countWeekdaysInSet(from: LocalDate, to: LocalDate, dates: Set<LocalDate>): Int {
    return dates.count { it in from..to && it.dayOfWeek != DayOfWeek.SATURDAY && it.dayOfWeek != DayOfWeek.SUNDAY }
}

private fun expandVacationDates(vacations: List<Vacation>, periodFrom: LocalDate, periodTo: LocalDate): Set<LocalDate> {
    val out = mutableSetOf<LocalDate>()
    vacations.forEach { v ->
        var d = maxOf(v.from, periodFrom); val end = minOf(v.to, periodTo)
        while (!d.isAfter(end)) { out.add(d); d = d.plusDays(1) }
    }
    return out
}

private fun buildVacationDescriptionsByDate(vacations: List<Vacation>, periodFrom: LocalDate, periodTo: LocalDate): Map<LocalDate, List<String>> {
    val map = mutableMapOf<LocalDate, MutableList<String>>()
    vacations.forEach { v ->
        var d = maxOf(v.from, periodFrom); val end = minOf(v.to, periodTo)
        while (!d.isAfter(end)) { map.getOrPut(d) { mutableListOf() }.add(v.description); d = d.plusDays(1) }
    }
    return map.mapValues { it.value.distinct() }
}

private fun formatCurrency(value: Double): String = String.format(Locale.getDefault(), "%.2f €", value)
private fun formatHours(value: Double): String = String.format(Locale.getDefault(), "%.1f h", value)
