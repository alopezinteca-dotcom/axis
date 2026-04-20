package vtsen.hashnode.dev.newemptycomposeapp.ui.activity

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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

    // -------------------------
    // Estado VM
    // -------------------------
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
            snackbarHostState.showSnackbar("✅ Import OK: ${r.travelsImported} viajes, ${r.stopsImported} paradas")
        }
    }

    // -------------------------
    // Archivo maestro por URI
    // -------------------------
    var masterFileUri by remember { mutableStateOf(ExportPreferences.getMasterFileUri(context)) }

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

        coroutineScope.launch { snackbarHostState.showSnackbar("✅ Maestro creado en Drive. Exportando…") }
    }

    fun launchCreateMaster() {
        createMasterLauncher.launch("AXIS_Master_Database.csv")
    }

    // -------------------------
    // Import CSV
    // -------------------------
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

    fun launchImportCsvPicker() {
        importCsvLauncher.launch(arrayOf("text/csv", "text/*", "application/octet-stream"))
    }

    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = { showImportConfirm = false },
            title = { Text("Importar CSV", fontWeight = FontWeight.Bold) },
            text = {
                Text("⚠️ REEMPLAZA TODO: borra datos locales y restaura desde el CSV.\n\n¿Continuar?")
            },
            confirmButton = {
                TextButton(onClick = {
                    val uri = pendingImportUri
                    pendingImportUri = null
                    showImportConfirm = false
                    if (uri != null) {
                        viewModel.importFromDriveCsv(uri, AxisImportCoordinator.ImportStrategy.REPLACE_ALL)
                        coroutineScope.launch { snackbarHostState.showSnackbar("⏳ Importando…") }
                    }
                }) { Text("Importar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingImportUri = null
                    showImportConfirm = false
                }) { Text("Cancelar") }
            }
        )
    }

    // =========================
    // 1) Periodo (Desde/Hasta) con persistencia
    // =========================
    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }

    val fromPickerState = rememberDatePickerState(
        initialSelectedDateMillis = BillingPeriodStore.localDateStartMillis(fromDate, zone)
    )
    val toPickerState = rememberDatePickerState(
        initialSelectedDateMillis = BillingPeriodStore.localDateStartMillis(toDate, zone)
    )

    // Mantener picker sincronizado con el periodo actual (evita errores raros)
    LaunchedEffect(fromDate) {
        fromPickerState.selectedDateMillis = BillingPeriodStore.localDateStartMillis(fromDate, zone)
    }
    LaunchedEffect(toDate) {
        toPickerState.selectedDateMillis = BillingPeriodStore.localDateStartMillis(toDate, zone)
    }

    if (showFromPicker) {
        DatePickerDialog(
            onDismissRequest = { showFromPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val picked = fromPickerState.selectedDateMillis
                    if (picked != null) {
                        val d = BillingPeriodStore.millisToLocalDateOrToday(picked, zone)
                        val newTo = if (d.isAfter(toDate)) d else toDate
                        viewModel.setBillingPeriodDates(d, newTo) // ✅ queda guardado
                    }
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
                    val picked = toPickerState.selectedDateMillis
                    if (picked != null) {
                        val d = BillingPeriodStore.millisToLocalDateOrToday(picked, zone)
                        val newFrom = if (d.isBefore(fromDate)) d else fromDate
                        viewModel.setBillingPeriodDates(newFrom, d) // ✅ queda guardado
                    }
                    showToPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showToPicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = toPickerState) }
    }

    // =========================
    // 2) Festivos y Vacaciones (con input arreglado)
    // =========================
    val allHolidays by CalendarOverridesStore.holidaysFlow(context).collectAsStateWithLifecycle(initialValue = emptyList())
    val allVacations by CalendarOverridesStore.vacationsFlow(context).collectAsStateWithLifecycle(initialValue = emptyList())

    var showCalendarManager by remember { mutableStateOf(false) }
    var showAddHoliday by remember { mutableStateOf(false) }
    var showAddVacation by remember { mutableStateOf(false) }

    var holidayDesc by rememberSaveable { mutableStateOf("") }
    var vacationDesc by rememberSaveable { mutableStateOf("") }

    val holidaysInPeriod by remember(allHolidays, fromDate, toDate) {
        derivedStateOf { allHolidays.filter { it.date in fromDate..toDate } }
    }
    val holidayDatesInPeriod by remember(holidaysInPeriod) {
        derivedStateOf { holidaysInPeriod.map { it.date }.toSet() }
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

    if (showCalendarManager) {
        CalendarManagementDialog(
            holidays = allHolidays,
            vacations = allVacations,
            onAddHoliday = { showAddHoliday = true },
            onAddVacation = { showAddVacation = true },
            onDeleteHoliday = { h ->
                val raw = CalendarOverridesStore.toRawHoliday(h)
                coroutineScope.launch { CalendarOverridesStore.removeHolidayRaw(context, raw) }
            },
            onDeleteVacation = { v ->
                val raw = CalendarOverridesStore.toRawVacation(v)
                coroutineScope.launch { CalendarOverridesStore.removeVacationRaw(context, raw) }
            },
            onDismiss = { showCalendarManager = false }
        )
    }

    // ---- Dialog añadir festivo (arreglado input) ----
    if (showAddHoliday) {
        val focusRequester = remember { FocusRequester() }
        val keyboard = LocalSoftwareKeyboardController.current

        AlertDialog(
            onDismissRequest = { showAddHoliday = false },
            title = { Text("Añadir festivo", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = holidayDesc,
                        onValueChange = { holidayDesc = it },
                        label = { Text("Descripción (festivo)") },
                        modifier = Modifier.fillMaxWidth()
                            .focusRequester(focusRequester),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        singleLine = true
                    )
                    DatePicker(state = holidayPickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val picked = holidayPickerState.selectedDateMillis
                    if (picked != null) {
                        val d = BillingPeriodStore.millisToLocalDateOrToday(picked, zone)
                        coroutineScope.launch {
                            CalendarOverridesStore.addHoliday(context, d, holidayDesc)
                            holidayDesc = ""
                            snackbarHostState.showSnackbar("✅ Festivo añadido")
                        }
                    }
                    showAddHoliday = false
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showAddHoliday = false }) { Text("Cancelar") } }
        )

        LaunchedEffect(showAddHoliday) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }

    // ---- Dialog añadir vacaciones (arreglado input) ----
    if (showAddVacation) {
        val focusRequester = remember { FocusRequester() }
        val keyboard = LocalSoftwareKeyboardController.current

        AlertDialog(
            onDismissRequest = { showAddVacation = false },
            title = { Text("Añadir vacaciones", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = vacationDesc,
                        onValueChange = { vacationDesc = it },
                        label = { Text("Descripción (vacaciones)") },
                        modifier = Modifier.fillMaxWidth()
                            .focusRequester(focusRequester),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        singleLine = true
                    )
                    Text("Desde", fontWeight = FontWeight.SemiBold)
                    DatePicker(state = vacFromPickerState)
                    Text("Hasta", fontWeight = FontWeight.SemiBold)
                    DatePicker(state = vacToPickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val aMillis = vacFromPickerState.selectedDateMillis
                    val bMillis = vacToPickerState.selectedDateMillis
                    if (aMillis != null && bMillis != null) {
                        val a = BillingPeriodStore.millisToLocalDateOrToday(aMillis, zone)
                        val b = BillingPeriodStore.millisToLocalDateOrToday(bMillis, zone)
                        coroutineScope.launch {
                            CalendarOverridesStore.addVacation(context, a, b, vacationDesc)
                            vacationDesc = ""
                            snackbarHostState.showSnackbar("✅ Vacaciones añadidas")
                        }
                    }
                    showAddVacation = false
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showAddVacation = false }) { Text("Cancelar") } }
        )

        LaunchedEffect(showAddVacation) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }

    // =========================
    // 3) KPIs (1–9) como pides
    // =========================

    // Días laborables: L-V menos festivos y vacaciones
    val diasLaborablesPeriodo by remember(fromDate, toDate, holidayDatesInPeriod, vacationDatesInPeriod) {
        derivedStateOf {
            countWeekdaysInclusive(fromDate, toDate) -
                countWeekdaysInSet(fromDate, toDate, holidayDatesInPeriod) -
                countWeekdaysInSet(fromDate, toDate, vacationDatesInPeriod)
        }
    }
    val diasLaborables = diasLaborablesPeriodo.coerceAtLeast(0)

    // Objetivos
    val totalFacturarObjetivo by remember(diasLaborables) { derivedStateOf { 350.0 * diasLaborables } }
    val totalHorasObjetivo by remember(diasLaborables) { derivedStateOf { 8.0 * diasLaborables } }

    // Total KM periodo (solo CERRADOS)
    val totalKmPeriodo by remember(periodTravels) {
        derivedStateOf {
            periodTravels.filter { it.status == TravelStatus.CLOSED }
                .sumOf { t -> ((t.kmEnd ?: t.kmStart) - t.kmStart).coerceAtLeast(0) }
        }
    }

    // Totales reales periodo
    val totalEstimadoPeriodo by remember(periodTravels) {
        derivedStateOf { periodTravels.sumOf { it.billingExpected } }
    }
    val facturadoPeriodo by remember(periodTravels) {
        derivedStateOf { periodTravels.filter { it.isInvoiced }.sumOf { it.billingExpected } }
    }
    val pendienteEstimadoPeriodo by remember(periodTravels) {
        derivedStateOf { periodTravels.filter { !it.isInvoiced }.sumOf { it.billingExpected } }
    }
    val horasCerradasPeriodo by remember(periodTravels) {
        derivedStateOf {
            periodTravels.filter { it.status == TravelStatus.CLOSED }
                .sumOf { it.hoursImputed ?: 0.0 }
        }
    }

    // Cartera global
    val pendienteCartera by remember(allTravels) {
        derivedStateOf { allTravels.filter { !it.isInvoiced }.sumOf { it.billingExpected } }
    }
    // KPI8 = A: horas de NO facturados (cerrados: hoursImputed; en curso: hoursDraft si existe)
    val horasCartera by remember(allTravels) {
        derivedStateOf {
            allTravels.filter { !it.isInvoiced }.sumOf { t ->
                when {
                    t.status == TravelStatus.CLOSED -> t.hoursImputed ?: 0.0
                    else -> t.hoursDraft ?: 0.0
                }
            }
        }
    }

    // Facturación anual (facturado SI del año en curso)
    val yearStartMillis = remember {
        BillingPeriodStore.localDateStartMillis(LocalDate.now().with(TemporalAdjusters.firstDayOfYear()), zone)
    }
    val yearEndMillis = remember {
        BillingPeriodStore.localDateEndMillis(LocalDate.now(), zone)
    }
    val facturacionAnual by remember(allTravels, yearStartMillis, yearEndMillis) {
        derivedStateOf {
            allTravels.filter { it.isInvoiced && it.startTimestamp in yearStartMillis..yearEndMillis }
                .sumOf { it.billingExpected }
        }
    }

    // =========================
    // 4) Timeline (optimizado)
    // =========================
    val daysInRange by remember(fromDate, toDate) { derivedStateOf { datesBetweenInclusive(fromDate, toDate) } }

    val travelsByDay by remember(periodTravels, zone) {
        derivedStateOf {
            periodTravels.groupBy { t -> BillingPeriodStore.millisToLocalDateOrToday(t.startTimestamp, zone) }
        }
    }
    val stopsCountByDay by remember(stopsInPeriod, zone) {
        derivedStateOf {
            stopsInPeriod.groupBy { s -> BillingPeriodStore.millisToLocalDateOrToday(s.timestamp, zone) }
                .mapValues { it.value.size }
        }
    }

    // =========================
    // 5) Edit/Delete + Facturado toggle
    // =========================
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var travelToDeleteId by remember { mutableStateOf<String?>(null) }

    var showEditDialog by remember { mutableStateOf(false) }
    var editingTravelId by remember { mutableStateOf<String?>(null) }
    var editWarning by remember { mutableStateOf<String?>(null) }

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
        editWarning = null
        showEditDialog = true
    }

    if (showDeleteConfirm && travelToDeleteId != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Eliminar viaje") },
            text = { Text("⚠️ Se eliminará el viaje y todas sus paradas. ¿Continuar?") },
            confirmButton = {
                TextButton(onClick = {
                    travelToDeleteId?.let { viewModel.deleteTravel(it) }
                    travelToDeleteId = null
                    showDeleteConfirm = false
                }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") } }
        )
    }

    if (showEditDialog && editingTravelId != null) {
        val original = allTravels.firstOrNull { it.id == editingTravelId }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Editar viaje", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    original?.let {
                        if (it.status == TravelStatus.CLOSED) {
                            Text("⚠️ Editando viaje CERRADO (histórico).", color = MaterialTheme.colorScheme.error)
                        }
                    }

                    OutlinedTextField(editOrigin, { editOrigin = it }, label = { Text("Origen") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(editDestination, { editDestination = it }, label = { Text("Destino") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(editDescription, { editDescription = it }, label = { Text("Descripción / Ref") }, modifier = Modifier.fillMaxWidth())

                    OutlinedTextField(
                        value = editBilling,
                        onValueChange = { editBilling = it },
                        label = { Text("Facturación (€)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Dieta", fontWeight = FontWeight.SemiBold)
                        Switch(checked = editHasDiet, onCheckedChange = { editHasDiet = it })
                    }

                    OutlinedTextField(
                        value = editKmStart,
                        onValueChange = { editKmStart = it.filter(Char::isDigit) },
                        label = { Text("KM inicio") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editKmEnd,
                        onValueChange = { editKmEnd = it.filter(Char::isDigit) },
                        label = { Text("KM fin (si cerrado)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editHoursImputed,
                        onValueChange = { editHoursImputed = it },
                        label = { Text("Horas imputadas (si cerrado)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Facturado", fontWeight = FontWeight.SemiBold)
                        Switch(checked = editIsInvoiced, onCheckedChange = { editIsInvoiced = it })
                    }

                    editWarning?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val base = original ?: run { showEditDialog = false; return@TextButton }

                    val kmStart = editKmStart.toIntOrNull()
                    if (kmStart == null || kmStart <= 0) { editWarning = "KM inicio inválido."; return@TextButton }

                    val kmEnd = editKmEnd.toIntOrNull()
                    if (editKmEnd.isNotBlank() && kmEnd == null) { editWarning = "KM fin inválido."; return@TextButton }
                    if (kmEnd != null && kmEnd < kmStart) { editWarning = "KM fin < KM inicio."; return@TextButton }

                    val billing = editBilling.replace(',', '.').toDoubleOrNull()
                    if (billing == null || billing < 0.0) { editWarning = "Facturación inválida."; return@TextButton }

                    val imputed = editHoursImputed.replace(',', '.').toDoubleOrNull()
                    if (editHoursImputed.isNotBlank() && imputed == null) { editWarning = "Horas imputadas inválidas."; return@TextButton }

                    val hoursCalc = base.hoursCalculatedSnapshot
                    val delta = if (imputed != null && hoursCalc != null) (imputed - hoursCalc) else base.deltaHours
                    val modified = if (delta != null) abs(delta) > 0.01 else base.hoursModified
                    val costeHora = base.snapCosteHoraAlejandro ?: 26.0
                    val impact = if (delta != null) delta * costeHora else base.impactEuroAlejandro

                    val updated = base.copy(
                        origin = editOrigin.trim(),
                        destination = editDestination.trim(),
                        description = editDescription.trim(),
                        billingExpected = billing,
                        hasDiet = editHasDiet,
                        kmStart = kmStart,
                        kmEnd = if (editKmEnd.isBlank()) base.kmEnd else kmEnd,
                        hoursImputed = if (base.status == TravelStatus.CLOSED) imputed else base.hoursImputed,
                        deltaHours = delta,
                        hoursModified = modified,
                        impactEuroAlejandro = impact,
                        isInvoiced = editIsInvoiced
                    )

                    viewModel.updateTravel(updated)
                    editWarning = null
                    showEditDialog = false
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { editWarning = null; showEditDialog = false }) { Text("Cancelar") } }
        )
    }

    // =========================
    // Scaffold
    // =========================
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("AXIS · Activity") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                actions = {
                    TextButton(onClick = { showCalendarManager = true }) {
                        Text("📅", fontSize = MaterialTheme.typography.titleLarge.fontSize)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewTravelClick, containerColor = MaterialTheme.colorScheme.primary) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }
        }
    ) { padding ->

        Row(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // =========================
            // IZQUIERDA: KPIs + Export/Import
            // =========================
            Column(
                modifier = Modifier.weight(0.35f).fillMaxHeight().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Resumen", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

                BlockTitle("Periodo (Desde / Hasta)")
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(onClick = { showFromPicker = true }, modifier = Modifier.weight(1f)) {
                                Text("Desde: ${fromDate.format(dateFormatter)}")
                            }
                            Button(onClick = { showToPicker = true }, modifier = Modifier.weight(1f)) {
                                Text("Hasta: ${toDate.format(dateFormatter)}")
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(onClick = { viewModel.resetBillingToThisMonth() }, modifier = Modifier.weight(1f)) { Text("Este mes") }
                            Button(onClick = { viewModel.resetBillingToThisMonth() }, modifier = Modifier.weight(1f)) { Text("Reset") }
                        }
                    }
                }

                BlockTitle("Días laborables (L-V menos festivos y vacaciones)")
                KpiSimple("Días laborables periodo", diasLaborables.toString())

                SectionDivider()

                // BLOQUE 1
                BlockTitle("Objetivos (Teórico)")
                KpiSimple("1) Total facturar periodo", formatCurrency(totalFacturarObjetivo))
                KpiSimple("2) Total KM periodo (solo cerrados)", "$totalKmPeriodo km")
                KpiSimple("3) Total horas periodo (objetivo)", formatHours(totalHorasObjetivo))

                SectionDivider()

                // BLOQUE 2
                BlockTitle("Comparativas (Real vs Objetivo)")
                KpiRatio(
                    title = "4) Facturación (facturado SI / objetivo)",
                    numerator = facturadoPeriodo,
                    denominator = totalFacturarObjetivo
                )
                KpiRatio(
                    title = "5) Horas (cerrados / objetivo)",
                    numerator = horasCerradasPeriodo,
                    denominator = totalHorasObjetivo
                )
                // KPI6 sin color (como pediste)
                KpiNoColorRatio(
                    title = "6) Estimado pendiente (NO facturado / total estimado periodo)",
                    numerator = pendienteEstimadoPeriodo,
                    denominator = totalEstimadoPeriodo
                )

                SectionDivider()

                // BLOQUE 3
                BlockTitle("Cartera (Global)")
                KpiSimple("7) Total pendiente cartera (NO facturado)", formatCurrency(pendienteCartera))
                KpiSimple("8) Total horas cartera (NO facturado)", formatHours(horasCartera))

                SectionDivider()

                // BLOQUE 4
                BlockTitle("Anual")
                KpiSimple("9) Facturación anual (facturado SI)", formatCurrency(facturacionAnual))

                Spacer(Modifier.height(16.dp))

                // Drive master buttons
                Button(
                    onClick = { launchCreateMaster() },
                    enabled = !isExporting && !isImporting,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("☁️ CONFIGURAR DRIVE (CREAR MAESTRO)", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val uri = masterFileUri
                        if (uri == null) {
                            coroutineScope.launch { snackbarHostState.showSnackbar("⚠️ Primero crea/configura el Maestro en Drive.") }
                        } else {
                            viewModel.exportToMasterFileUri(uri)
                        }
                    },
                    enabled = !isExporting && !isImporting,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    if (isExporting) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator(strokeWidth = 2.dp)
                            Text("EXPORTANDO…", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text("📤 EXPORTAR A MAESTRO (DRIVE)", fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = { launchImportCsvPicker() },
                    enabled = !isExporting && !isImporting,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    if (isImporting) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator(strokeWidth = 2.dp)
                            Text("IMPORTANDO…", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text("📥 IMPORTAR CSV (DRIVE)", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // =========================
            // DERECHA: Timeline
            // =========================
            Column(
                modifier = Modifier.weight(0.65f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Timeline del periodo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                    items(daysInRange, key = { it.toEpochDay() }) { day ->
                        val isWeekend = day.isWeekend()
                        val holiday: Holiday? = holidayByDateInPeriod[day]
                        val isHoliday = holiday != null
                        val vacationDescs = vacationDescsByDateInPeriod[day].orEmpty()
                        val isVacation = vacationDescs.isNotEmpty()

                        DayHeader(day, dateFormatter, localeEs, isWeekend, isHoliday, isVacation)

                        if (holiday != null) {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("🎉 FESTIVO", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                    val desc = holiday.description.trim()
                                    if (desc.isNotBlank()) Text(desc, color = MaterialTheme.colorScheme.onErrorContainer)
                                }
                            }
                        } else if (isVacation) {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("🏖️ VACACIONES", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                    vacationDescs.map { it.trim() }.filter { it.isNotBlank() }.distinct().forEach {
                                        Text(it, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                    }
                                }
                            }
                        }

                        val stopCount = stopsCountByDay[day] ?: 0
                        if (stopCount > 0) {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer), modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("📍 $stopCount", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Viaje en curso
                        val current = currentTravel
                        val currentDay = current?.startTimestamp?.let { BillingPeriodStore.millisToLocalDateOrToday(it, zone) }
                        if (current != null && current.status == TravelStatus.IN_PROGRESS && currentDay == day) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                modifier = Modifier.fillMaxWidth().clickable { onCurrentTravelClick() }
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text("🟢 EN CURSO (tocar para continuar)", fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("${current.origin} → ${current.destination}", style = MaterialTheme.typography.titleMedium)
                                    Text("KM inicio: ${current.kmStart} | Draft: ${current.hoursDraft ?: 0.0}h")
                                }
                            }
                        }

                        // Viajes cerrados del día
                        val travelsToday = (travelsByDay[day] ?: emptyList())
                            .filter { it.status == TravelStatus.CLOSED }
                            .sortedBy { it.startTimestamp }

                        if (stopCount == 0 && currentDay != day && travelsToday.isEmpty()) {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(12.dp)) {
                                    Text("Sin viajes", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            travelsToday.forEach { tr ->
                                TravelRowCard(
                                    travel = tr,
                                    zone = zone,
                                    dateFormatter = dateFormatter,
                                    onToggleInvoiced = { checked -> viewModel.setFacturado(tr.id, checked) },
                                    onEdit = { openEditDialog(tr) },
                                    onDelete = { travelToDeleteId = tr.id; showDeleteConfirm = true }
                                )
                            }
                        }

                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

/* =========================
   UI Helpers (KPIs)
   ========================= */

@Composable
private fun BlockTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
}

@Composable
private fun SectionDivider() {
    Spacer(modifier = Modifier.height(10.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f))
    Spacer(modifier = Modifier.height(10.dp))
}

@Composable
private fun KpiSimple(title: String, value: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(10.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun KpiRatio(title: String, numerator: Double, denominator: Double) {
    val ok = numerator >= denominator
    val color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val left = formatCurrencyOrHoursAuto(title, numerator)
    val right = formatCurrencyOrHoursAuto(title, denominator)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(left, fontWeight = FontWeight.Bold, color = color)
                Text(right, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun KpiNoColorRatio(title: String, numerator: Double, denominator: Double) {
    val left = formatCurrencyOrHoursAuto(title, numerator)
    val right = formatCurrencyOrHoursAuto(title, denominator)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(left, fontWeight = FontWeight.Bold)
                Text(right, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun formatCurrencyOrHoursAuto(title: String, value: Double): String {
    return if (title.contains("Horas", ignoreCase = true)) formatHours(value) else formatCurrency(value)
}

/* =========================
   Timeline card (Facturado SI/NO incluido)
   ========================= */

@Composable
private fun TravelRowCard(
    travel: TravelEntity,
    zone: ZoneId,
    dateFormatter: DateTimeFormatter,
    onToggleInvoiced: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val warning = if (travel.hoursModified) " ⚠️" else ""
    val date = BillingPeriodStore.millisToLocalDateOrToday(travel.startTimestamp, zone).format(dateFormatter)

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f).clickable { onEdit() }) {
                    Text("$date · ${travel.origin} → ${travel.destination}$warning", fontWeight = FontWeight.SemiBold)
                    if (travel.description.isNotBlank()) {
                        Text("Ref: ${travel.description}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onEdit) { Text("✏️") }
                    TextButton(onClick = onDelete) { Text("🗑️") }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Horas: ${travel.hoursImputed?.let { formatHours(it) } ?: "—"}")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Fact.", style = MaterialTheme.typography.labelMedium)
                    Checkbox(
                        checked = travel.isInvoiced,
                        onCheckedChange = { onToggleInvoiced(it) },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }

            Text("€ ${formatCurrencyNumber(travel.billingExpected)}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

/* =========================
   Date helpers
   ========================= */

private fun datesBetweenInclusive(from: LocalDate, to: LocalDate): List<LocalDate> {
    if (to.isBefore(from)) return emptyList()
    val out = ArrayList<LocalDate>()
    var d = from
    while (!d.isAfter(to)) {
        out.add(d)
        d = d.plusDays(1)
    }
    return out
}

private fun LocalDate.isWeekend(): Boolean =
    this.dayOfWeek == DayOfWeek.SATURDAY || this.dayOfWeek == DayOfWeek.SUNDAY

private fun countWeekdaysInclusive(from: LocalDate, to: LocalDate): Int {
    if (to.isBefore(from)) return 0
    var d = from
    var count = 0
    while (!d.isAfter(to)) {
        if (!d.isWeekend()) count++
        d = d.plusDays(1)
    }
    return count
}

private fun countWeekdaysInSet(from: LocalDate, to: LocalDate, dates: Set<LocalDate>): Int {
    if (dates.isEmpty()) return 0
    return dates.count { it in from..to && !it.isWeekend() }
}

private fun expandVacationDates(vacations: List<Vacation>, periodFrom: LocalDate, periodTo: LocalDate): Set<LocalDate> {
    if (vacations.isEmpty()) return emptySet()
    val out = mutableSetOf<LocalDate>()
    vacations.forEach { v ->
        var d = maxOf(v.from, periodFrom)
        val end = minOf(v.to, periodTo)
        while (!d.isAfter(end)) {
            out.add(d)
            d = d.plusDays(1)
        }
    }
    return out
}

private fun buildVacationDescriptionsByDate(
    vacations: List<Vacation>,
    periodFrom: LocalDate,
    periodTo: LocalDate
): Map<LocalDate, List<String>> {
    if (vacations.isEmpty()) return emptyMap()
    val map = mutableMapOf<LocalDate, MutableList<String>>()
    vacations.forEach { v ->
        var d = maxOf(v.from, periodFrom)
        val end = minOf(v.to, periodTo)
        while (!d.isAfter(end)) {
            map.getOrPut(d) { mutableListOf() }.add(v.description)
            d = d.plusDays(1)
        }
    }
    return map.mapValues { (_, v) -> v.map { it.trim() }.filter { it.isNotBlank() }.distinct() }
}

private fun capitalizeFirst(s: String): String =
    s.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "ES")) else it.toString() }

@Composable
private fun DayHeader(
    day: LocalDate,
    dateFormatter: DateTimeFormatter,
    localeEs: Locale,
    isWeekend: Boolean,
    isHoliday: Boolean,
    isVacation: Boolean
) {
    val weekdayName = remember(day) {
        capitalizeFirst(day.dayOfWeek.getDisplayName(TextStyle.FULL, localeEs))
    }

    val containerColor = when {
        isHoliday || isWeekend -> MaterialTheme.colorScheme.errorContainer
        isVacation -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    val titleColor = when {
        isHoliday || isWeekend -> MaterialTheme.colorScheme.error
        isVacation -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    Card(colors = CardDefaults.cardColors(containerColor = containerColor), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("$weekdayName ${day.dayOfMonth}", fontWeight = FontWeight.Bold, color = titleColor)
                Text(day.format(dateFormatter), fontWeight = FontWeight.SemiBold, color = titleColor.copy(alpha = 0.9f))
            }
        }
    }
}

private fun formatCurrency(value: Double): String = "${formatCurrencyNumber(value)} €"
private fun formatCurrencyNumber(value: Double): String = String.format(Locale.getDefault(), "%.2f", value)
private fun formatHours(value: Double): String = String.format(Locale.getDefault(), "%.1f h", value)
