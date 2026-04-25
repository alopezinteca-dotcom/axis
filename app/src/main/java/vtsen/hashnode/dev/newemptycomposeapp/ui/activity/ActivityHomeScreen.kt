package vtsen.hashnode.dev.newemptycomposeapp.ui.activity

import android.content.Intent
import android.net.Uri
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
    val context    = LocalContext.current
    val zone       = remember { ZoneId.systemDefault() }
    val localeEs   = remember { Locale("es", "ES") }
    val dateFmt    = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    val coroutine  = rememberCoroutineScope()
    val snackbar   = remember { SnackbarHostState() }

    // ── Estado VM ──────────────────────────────────────────────────────────
    val allTravels      by viewModel.allTravels.collectAsStateWithLifecycle(initialValue = emptyList())
    val currentTravel   by viewModel.currentTravel.collectAsStateWithLifecycle(initialValue = null)
    val stopsInPeriod   by viewModel.stopsInPeriod.collectAsStateWithLifecycle(initialValue = emptyList())
    val periodDates     by viewModel.periodDates.collectAsStateWithLifecycle(initialValue = LocalDate.now(zone) to LocalDate.now(zone))
    val fromDate = periodDates.first
    val toDate   = periodDates.second

    val isExporting     by viewModel.isExporting.collectAsStateWithLifecycle(initialValue = false)
    val exportError     by viewModel.exportError.collectAsStateWithLifecycle(initialValue = null)
    val isImporting     by viewModel.isImporting.collectAsStateWithLifecycle(initialValue = false)
    val importError     by viewModel.importError.collectAsStateWithLifecycle(initialValue = null)
    val lastImportResult by viewModel.lastImportResult.collectAsStateWithLifecycle(initialValue = null)

    LaunchedEffect(exportError)      { exportError?.let      { snackbar.showSnackbar("❌ Export: $it");   viewModel.clearExportError() } }
    LaunchedEffect(importError)      { importError?.let      { snackbar.showSnackbar("❌ Import: $it");   viewModel.clearImportError() } }
    LaunchedEffect(lastImportResult) { lastImportResult?.let { snackbar.showSnackbar("✅ Import OK: ${it.travelsImported} viajes, ${it.stopsImported} paradas") } }

    // ── Maestro (OneDrive / Drive / picker genérico) ───────────────────────
    var masterFileUri by remember { mutableStateOf(ExportPreferences.getMasterFileUri(context)) }

    val createMasterLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
        ExportPreferences.saveMasterFileUri(context, uri)
        masterFileUri = uri
        viewModel.exportToMasterFileUri(uri)
        coroutine.launch { snackbar.showSnackbar("✅ Maestro creado. Exportando…") }
    }

    // ── Import CSV ────────────────────────────────────────────────────────
    var pendingImportUri  by remember { mutableStateOf<Uri?>(null) }
    var showImportConfirm by remember { mutableStateOf(false) }

    val importCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        pendingImportUri  = uri
        showImportConfirm = true
    }

    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = { showImportConfirm = false },
            title = { Text("Importar CSV", fontWeight = FontWeight.Bold) },
            text  = { Text("⚠️ REEMPLAZA TODO: borra datos locales y restaura desde el CSV.\n\n¿Continuar?") },
            confirmButton = {
                TextButton(onClick = {
                    val uri = pendingImportUri
                    pendingImportUri  = null
                    showImportConfirm = false
                    if (uri != null) {
                        viewModel.importFromDriveCsv(uri, AxisImportCoordinator.ImportStrategy.REPLACE_ALL)
                        coroutine.launch { snackbar.showSnackbar("⏳ Importando…") }
                    }
                }) { Text("Importar") }
            },
            dismissButton = { TextButton(onClick = { pendingImportUri = null; showImportConfirm = false }) { Text("Cancelar") } }
        )
    }

    // ── Periodo Desde/Hasta ───────────────────────────────────────────────
    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker   by remember { mutableStateOf(false) }

    val fromPickerState = rememberDatePickerState(initialSelectedDateMillis = BillingPeriodStore.localDateStartMillis(fromDate, zone))
    val toPickerState   = rememberDatePickerState(initialSelectedDateMillis = BillingPeriodStore.localDateStartMillis(toDate,   zone))

    LaunchedEffect(fromDate) { fromPickerState.selectedDateMillis = BillingPeriodStore.localDateStartMillis(fromDate, zone) }
    LaunchedEffect(toDate)   { toPickerState.selectedDateMillis   = BillingPeriodStore.localDateStartMillis(toDate,   zone) }

    if (showFromPicker) {
        DatePickerDialog(
            onDismissRequest = { showFromPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    fromPickerState.selectedDateMillis?.let {
                        val d = BillingPeriodStore.millisToLocalDateOrToday(it, zone)
                        viewModel.setBillingPeriodDates(d, if (d.isAfter(toDate)) d else toDate)
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
                    toPickerState.selectedDateMillis?.let {
                        val d = BillingPeriodStore.millisToLocalDateOrToday(it, zone)
                        viewModel.setBillingPeriodDates(if (d.isBefore(fromDate)) d else fromDate, d)
                    }
                    showToPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showToPicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = toPickerState) }
    }

    // ── Calendario (Festivos / Vacaciones) ────────────────────────────────
    val allHolidays  by CalendarOverridesStore.holidaysFlow(context).collectAsStateWithLifecycle(initialValue = emptyList())
    val allVacations by CalendarOverridesStore.vacationsFlow(context).collectAsStateWithLifecycle(initialValue = emptyList())

    var showCalendarManager by remember { mutableStateOf(false) }
    var showAddHoliday      by remember { mutableStateOf(false) }
    var showAddVacation     by remember { mutableStateOf(false) }
    var holidayDesc         by rememberSaveable { mutableStateOf("") }
    var vacationDesc        by rememberSaveable { mutableStateOf("") }

    val holidayPickerState  = rememberDatePickerState(initialSelectedDateMillis = BillingPeriodStore.localDateStartMillis(LocalDate.now(), zone))
    val vacFromPickerState  = rememberDatePickerState(initialSelectedDateMillis = BillingPeriodStore.localDateStartMillis(LocalDate.now(), zone))
    val vacToPickerState    = rememberDatePickerState(initialSelectedDateMillis = BillingPeriodStore.localDateStartMillis(LocalDate.now(), zone))

    val holidaysInPeriod         by remember(allHolidays, fromDate, toDate)  { derivedStateOf { allHolidays.filter { it.date in fromDate..toDate } } }
    val holidayDatesInPeriod     by remember(holidaysInPeriod)                { derivedStateOf { holidaysInPeriod.map { it.date }.toSet() } }
    val holidayByDateInPeriod    by remember(holidaysInPeriod)                { derivedStateOf { holidaysInPeriod.associateBy { it.date } } }
    val vacationsInPeriod        by remember(allVacations, fromDate, toDate)  { derivedStateOf { allVacations.filter { v -> !(v.to.isBefore(fromDate) || v.from.isAfter(toDate)) } } }
    val vacationDatesInPeriod    by remember(vacationsInPeriod, fromDate, toDate) { derivedStateOf { expandVacationDates(vacationsInPeriod, fromDate, toDate) } }
    val vacationDescsByDate      by remember(vacationsInPeriod, fromDate, toDate) { derivedStateOf { buildVacationDescriptionsByDate(vacationsInPeriod, fromDate, toDate) } }

    if (showCalendarManager) {
        CalendarManagementDialog(
            holidays      = allHolidays,
            vacations     = allVacations,
            onAddHoliday  = { showAddHoliday  = true },
            onAddVacation = { showAddVacation = true },
            onDeleteHoliday  = { h -> coroutine.launch { CalendarOverridesStore.removeHolidayRaw(context,  CalendarOverridesStore.toRawHoliday(h)) } },
            onDeleteVacation = { v -> coroutine.launch { CalendarOverridesStore.removeVacationRaw(context, CalendarOverridesStore.toRawVacation(v)) } },
            onDismiss = { showCalendarManager = false }
        )
    }

    if (showAddHoliday) {
        val focus    = remember { FocusRequester() }
        val keyboard = LocalSoftwareKeyboardController.current
        AlertDialog(
            onDismissRequest = { showAddHoliday = false },
            title = { Text("Añadir festivo", fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = holidayDesc, onValueChange = { holidayDesc = it }, label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth().focusRequester(focus), singleLine = true)
                    DatePicker(state = holidayPickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    holidayPickerState.selectedDateMillis?.let {
                        val d = BillingPeriodStore.millisToLocalDateOrToday(it, zone)
                        coroutine.launch { CalendarOverridesStore.addHoliday(context, d, holidayDesc); holidayDesc = ""; snackbar.showSnackbar("✅ Festivo añadido") }
                    }
                    showAddHoliday = false
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showAddHoliday = false }) { Text("Cancelar") } }
        )
        LaunchedEffect(showAddHoliday) { focus.requestFocus(); keyboard?.show() }
    }

    if (showAddVacation) {
        val focus    = remember { FocusRequester() }
        val keyboard = LocalSoftwareKeyboardController.current
        AlertDialog(
            onDismissRequest = { showAddVacation = false },
            title = { Text("Añadir vacaciones", fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = vacationDesc, onValueChange = { vacationDesc = it }, label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth().focusRequester(focus), singleLine = true)
                    Text("Desde", fontWeight = FontWeight.SemiBold); DatePicker(state = vacFromPickerState)
                    Text("Hasta", fontWeight = FontWeight.SemiBold); DatePicker(state = vacToPickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val a = vacFromPickerState.selectedDateMillis?.let { BillingPeriodStore.millisToLocalDateOrToday(it, zone) }
                    val b = vacToPickerState.selectedDateMillis?.let   { BillingPeriodStore.millisToLocalDateOrToday(it, zone) }
                    if (a != null && b != null) {
                        coroutine.launch { CalendarOverridesStore.addVacation(context, a, b, vacationDesc); vacationDesc = ""; snackbar.showSnackbar("✅ Vacaciones añadidas") }
                    }
                    showAddVacation = false
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showAddVacation = false }) { Text("Cancelar") } }
        )
        LaunchedEffect(showAddVacation) { focus.requestFocus(); keyboard?.show() }
    }

    // ── Timeline: cálculo de rango ────────────────────────────────────────
    // ✅ Sin LaunchedEffect de setStopsRange — stopsInPeriod es reactivo desde billingPeriod
    val fromMillis = remember(fromDate) { BillingPeriodStore.localDateStartMillis(fromDate, zone) }
    val toMillis   = remember(toDate)   { BillingPeriodStore.localDateEndMillis(toDate,   zone) }

    val periodTravels  by remember(allTravels, fromMillis, toMillis) { derivedStateOf { allTravels.filter { it.startTimestamp in fromMillis..toMillis } } }
    val daysInRange    by remember(fromDate, toDate)  { derivedStateOf { datesBetweenInclusive(fromDate, toDate) } }
    val travelsByDay   by remember(periodTravels, zone) { derivedStateOf { periodTravels.groupBy { BillingPeriodStore.millisToLocalDateOrToday(it.startTimestamp, zone) } } }
    val stopsCountByDay by remember(stopsInPeriod, zone) {
        derivedStateOf {
            stopsInPeriod.groupBy { BillingPeriodStore.millisToLocalDateOrToday(it.timestamp, zone) }.mapValues { it.value.size }
        }
    }

    // ── KPIs ──────────────────────────────────────────────────────────────
    val diasLaborables by remember(fromDate, toDate, holidayDatesInPeriod, vacationDatesInPeriod) {
        derivedStateOf {
            (countWeekdaysInclusive(fromDate, toDate)
                - countWeekdaysInSet(fromDate, toDate, holidayDatesInPeriod)
                - countWeekdaysInSet(fromDate, toDate, vacationDatesInPeriod)).coerceAtLeast(0)
        }
    }
    val totalFacturarObjetivo by remember(diasLaborables)  { derivedStateOf { 350.0 * diasLaborables } }
    val totalHorasObjetivo    by remember(diasLaborables)  { derivedStateOf { 8.0   * diasLaborables } }
    val totalKmPeriodo        by remember(periodTravels)   { derivedStateOf { periodTravels.filter { it.status == TravelStatus.CLOSED }.sumOf { ((it.kmEnd ?: it.kmStart) - it.kmStart).coerceAtLeast(0) } } }
    val facturadoPeriodo      by remember(periodTravels)   { derivedStateOf { periodTravels.filter { it.isInvoiced }.sumOf { it.billingExpected } } }
    val horasCerradas         by remember(periodTravels)   { derivedStateOf { periodTravels.filter { it.status == TravelStatus.CLOSED }.sumOf { it.hoursImputed ?: 0.0 } } }
    val totalEstimado         by remember(periodTravels)   { derivedStateOf { periodTravels.sumOf { it.billingExpected } } }
    val pendienteEstimado     by remember(periodTravels)   { derivedStateOf { periodTravels.filter { !it.isInvoiced }.sumOf { it.billingExpected } } }
    val pendienteCartera      by remember(allTravels)      { derivedStateOf { allTravels.filter { !it.isInvoiced }.sumOf { it.billingExpected } } }
    val horasCartera          by remember(allTravels)      { derivedStateOf { allTravels.filter { !it.isInvoiced }.sumOf { if (it.status == TravelStatus.CLOSED) (it.hoursImputed ?: 0.0) else (it.hoursDraft ?: 0.0) } } }
    val yearStartMillis = remember { BillingPeriodStore.localDateStartMillis(LocalDate.now().with(TemporalAdjusters.firstDayOfYear()), zone) }
    val yearEndMillis   = remember { BillingPeriodStore.localDateEndMillis(LocalDate.now(), zone) }
    val facturacionAnual by remember(allTravels, yearStartMillis, yearEndMillis) { derivedStateOf { allTravels.filter { it.isInvoiced && it.startTimestamp in yearStartMillis..yearEndMillis }.sumOf { it.billingExpected } } }

    // ── UI ────────────────────────────────────────────────────────────────
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("AXIS · Activity") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                actions = { TextButton(onClick = { showCalendarManager = true }) { Text("📅", fontSize = MaterialTheme.typography.titleLarge.fontSize) } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewTravelClick, containerColor = MaterialTheme.colorScheme.primary) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }
        }
    ) { padding ->
        Row(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {

            // ── IZQUIERDA: KPIs + botones export ─────────────────────────
            Column(modifier = Modifier.weight(0.35f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Resumen", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

                BlockTitle("Periodo (Desde / Hasta)")
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(onClick = { showFromPicker = true }, modifier = Modifier.weight(1f)) { Text("Desde: ${fromDate.format(dateFmt)}") }
                            Button(onClick = { showToPicker   = true }, modifier = Modifier.weight(1f)) { Text("Hasta: ${toDate.format(dateFmt)}") }
                        }
                        Button(onClick = { viewModel.resetBillingToThisMonth() }, modifier = Modifier.fillMaxWidth()) { Text("📅 Forzar Mes Corriente") }
                    }
                }

                BlockTitle("Días laborables periodo")
                KpiSimple("L-V menos festivos y vacaciones", diasLaborables.toString())
                SectionDivider()

                BlockTitle("Objetivos (Teórico)")
                KpiSimple("1) Total facturar periodo",    formatCurrency(totalFacturarObjetivo))
                KpiSimple("2) Total KM periodo (cerrados)", "$totalKmPeriodo km")
                KpiSimple("3) Total horas periodo (objetivo)", formatHours(totalHorasObjetivo))
                SectionDivider()

                BlockTitle("Comparativas (Real vs Objetivo)")
                KpiRatioColored("4) Facturación (SI / objetivo)",    facturadoPeriodo,  totalFacturarObjetivo, isHours = false)
                KpiRatioColored("5) Horas (cerrados / objetivo)",    horasCerradas,     totalHorasObjetivo,    isHours = true)
                KpiRatioNoColor("6) Estimado pendiente (NO / total)", pendienteEstimado, totalEstimado,         isHours = false)
                SectionDivider()

                BlockTitle("Cartera (Global)")
                KpiSimple("7) Pendiente cartera (NO facturado)", formatCurrency(pendienteCartera))
                KpiSimple("8) Horas cartera (NO facturado)",     formatHours(horasCartera))
                SectionDivider()

                BlockTitle("Anual")
                KpiSimple("9) Facturación anual (SI facturado)", formatCurrency(facturacionAnual))

                Spacer(Modifier.height(16.dp))

                Button(onClick = { createMasterLauncher.launch("AXIS_Master_Database.csv") }, enabled = !isExporting && !isImporting, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text("☁️ CONFIGURAR DESTINO (CREAR MAESTRO)", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {
                        val uri = masterFileUri
                        if (uri == null) coroutine.launch { snackbar.showSnackbar("⚠️ Primero crea/configura el archivo Maestro.") }
                        else viewModel.exportToMasterFileUri(uri)
                    },
                    enabled = !isExporting && !isImporting,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    if (isExporting) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { CircularProgressIndicator(strokeWidth = 2.dp); Text("EXPORTANDO…", fontWeight = FontWeight.Bold) }
                    else Text("📤 EXPORTAR A MAESTRO", fontWeight = FontWeight.Bold)
                }
                Button(onClick = { importCsvLauncher.launch(arrayOf("text/csv", "text/*", "application/octet-stream")) }, enabled = !isExporting && !isImporting, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    if (isImporting) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { CircularProgressIndicator(strokeWidth = 2.dp); Text("IMPORTANDO…", fontWeight = FontWeight.Bold) }
                    else Text("📥 IMPORTAR CSV", fontWeight = FontWeight.Bold)
                }
            }

            // ── DERECHA: Timeline ─────────────────────────────────────────
            Column(modifier = Modifier.weight(0.65f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Timeline del periodo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                    items(daysInRange, key = { it.toEpochDay() }) { day ->
                        val isWeekend    = day.isWeekend()
                        val holiday      = holidayByDateInPeriod[day]
                        val isHoliday    = holiday != null
                        val vacDescs     = vacationDescsByDate[day].orEmpty()
                        val isVacation   = vacDescs.isNotEmpty()

                        DayHeader(day, dateFmt, localeEs, isWeekend, isHoliday, isVacation)

                        if (holiday != null) {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("🎉 FESTIVO", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                    if (holiday.description.trim().isNotBlank()) Text(holiday.description.trim(), color = MaterialTheme.colorScheme.onErrorContainer)
                                }
                            }
                        } else if (isVacation) {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("🏖️ VACACIONES", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                    vacDescs.distinct().filter { it.isNotBlank() }.forEach { Text(it, color = MaterialTheme.colorScheme.onTertiaryContainer) }
                                }
                            }
                        }

                        val stopCount = stopsCountByDay[day] ?: 0
                        if (stopCount > 0) {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer), modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Text("📍 $stopCount parada(s)", fontWeight = FontWeight.Bold) }
                            }
                        }

                        // ✅ Viaje EN CURSO: se muestra su propia tarjeta especial si cae en este día
                        val curTravel = currentTravel
                        val curDay    = curTravel?.startTimestamp?.let { BillingPeriodStore.millisToLocalDateOrToday(it, zone) }
                        if (curTravel != null && curDay == day) {
                            Card(
                                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                modifier = Modifier.fillMaxWidth().clickable { onCurrentTravelClick() }
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text("🟢 EN CURSO (tocar para continuar)", fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(4.dp))
                                    Text("${curTravel.origin} → ${curTravel.destination}", style = MaterialTheme.typography.titleMedium)
                                    Text("KM inicio: ${curTravel.kmStart} | Draft: ${curTravel.hoursDraft ?: 0.0}h")
                                }
                            }
                        }

                        // ✅ CAMBIO PRINCIPAL: muestra TODOS los viajes del día (sin filtro CLOSED),
                        //    excepto el EN CURSO que ya tiene su tarjeta especial arriba.
                        val travelsToday = (travelsByDay[day] ?: emptyList())
                            .filter { it.id != curTravel?.id }   // evita duplicado del EN CURSO
                            .sortedBy { it.startTimestamp }

                        if (stopCount == 0 && curDay != day && travelsToday.isEmpty()) {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(12.dp)) { Text("Sin viajes", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                        } else {
                            travelsToday.forEach { tr ->
                                TravelRowCard(
                                    travel           = tr,
                                    zone             = zone,
                                    dateFormatter    = dateFmt,
                                    onToggleInvoiced = { viewModel.setFacturado(tr.id, it) },
                                    // ✅ Click inteligente: CLOSED → TravelEdit, cualquier otro → TravelDetail
                                    onEdit = {
                                        if (tr.status == TravelStatus.CLOSED) {
                                            onEditTravelClick(tr.id)
                                        } else {
                                            onCurrentTravelClick()
                                        }
                                    }
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

/* ── KPI helpers ────────────────────────────────────────────────────────── */

@Composable private fun BlockTitle(text: String) = Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

@Composable private fun SectionDivider() {
    Spacer(Modifier.height(10.dp)); HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)); Spacer(Modifier.height(10.dp))
}

@Composable private fun KpiSimple(title: String, value: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp)) { Text(title, style = MaterialTheme.typography.labelMedium); Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
    }
}

@Composable private fun KpiRatioColored(title: String, numerator: Double, denominator: Double, isHours: Boolean) {
    val ok    = numerator >= denominator
    val color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val left  = if (isHours) formatHours(numerator)   else formatCurrency(numerator)
    val right = if (isHours) formatHours(denominator) else formatCurrency(denominator)
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(left, fontWeight = FontWeight.Bold, color = color); Text(right, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable private fun KpiRatioNoColor(title: String, numerator: Double, denominator: Double, isHours: Boolean) {
    val left  = if (isHours) formatHours(numerator)   else formatCurrency(numerator)
    val right = if (isHours) formatHours(denominator) else formatCurrency(denominator)
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(left, fontWeight = FontWeight.Bold); Text(right, fontWeight = FontWeight.Bold) }
        }
    }
}

/* ── Travel card ────────────────────────────────────────────────────────── */

@Composable
private fun TravelRowCard(
    travel: TravelEntity,
    zone: ZoneId,
    dateFormatter: DateTimeFormatter,
    onToggleInvoiced: (Boolean) -> Unit,
    onEdit: () -> Unit
) {
    val date = BillingPeriodStore.millisToLocalDateOrToday(travel.startTimestamp, zone).format(dateFormatter)

    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth().clickable { onEdit() }) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("$date · ${travel.origin} → ${travel.destination}", fontWeight = FontWeight.SemiBold)
            if (travel.description.isNotBlank()) Text("Ref: ${travel.description}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Horas: ${travel.hoursImputed?.let { formatHours(it) } ?: "—"}")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Fact.", style = MaterialTheme.typography.labelMedium)
                    Checkbox(checked = travel.isInvoiced, onCheckedChange = { onToggleInvoiced(it) }, colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary))
                }
            }
            Text("€ ${formatCurrencyNumber(travel.billingExpected)}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            // Indicador de acción según estado
            Text(
                if (travel.status == TravelStatus.CLOSED) "📝 Detalle / Editar" else "🟢 Ver viaje en curso",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/* ── Date helpers ───────────────────────────────────────────────────────── */

private fun datesBetweenInclusive(from: LocalDate, to: LocalDate): List<LocalDate> {
    if (to.isBefore(from)) return emptyList()
    val out = ArrayList<LocalDate>()
    var d = from
    while (!d.isAfter(to)) { out.add(d); d = d.plusDays(1) }
    return out
}

private fun LocalDate.isWeekend() = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY

private fun countWeekdaysInclusive(from: LocalDate, to: LocalDate): Int {
    if (to.isBefore(from)) return 0
    var d = from; var n = 0
    while (!d.isAfter(to)) { if (!d.isWeekend()) n++; d = d.plusDays(1) }
    return n
}

private fun countWeekdaysInSet(from: LocalDate, to: LocalDate, dates: Set<LocalDate>) =
    if (dates.isEmpty()) 0 else dates.count { it in from..to && !it.isWeekend() }

private fun expandVacationDates(vacations: List<Vacation>, periodFrom: LocalDate, periodTo: LocalDate): Set<LocalDate> {
    if (vacations.isEmpty()) return emptySet()
    val out = mutableSetOf<LocalDate>()
    vacations.forEach { v ->
        var d = maxOf(v.from, periodFrom); val end = minOf(v.to, periodTo)
        while (!d.isAfter(end)) { out.add(d); d = d.plusDays(1) }
    }
    return out
}

private fun buildVacationDescriptionsByDate(vacations: List<Vacation>, periodFrom: LocalDate, periodTo: LocalDate): Map<LocalDate, List<String>> {
    if (vacations.isEmpty()) return emptyMap()
    val map = mutableMapOf<LocalDate, MutableList<String>>()
    vacations.forEach { v ->
        var d = maxOf(v.from, periodFrom); val end = minOf(v.to, periodTo)
        while (!d.isAfter(end)) { map.getOrPut(d) { mutableListOf() }.add(v.description); d = d.plusDays(1) }
    }
    return map.mapValues { (_, v) -> v.map { it.trim() }.filter { it.isNotBlank() }.distinct() }
}

private fun capitalizeFirst(s: String) = s.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "ES")) else it.toString() }

@Composable
private fun DayHeader(day: LocalDate, dateFormatter: DateTimeFormatter, localeEs: Locale, isWeekend: Boolean, isHoliday: Boolean, isVacation: Boolean) {
    val weekdayName = remember(day) { capitalizeFirst(day.dayOfWeek.getDisplayName(TextStyle.FULL, localeEs)) }
    val containerColor = when { isHoliday || isWeekend -> MaterialTheme.colorScheme.errorContainer; isVacation -> MaterialTheme.colorScheme.tertiaryContainer; else -> MaterialTheme.colorScheme.primaryContainer }
    val titleColor     = when { isHoliday || isWeekend -> MaterialTheme.colorScheme.error;           isVacation -> MaterialTheme.colorScheme.onTertiaryContainer; else -> MaterialTheme.colorScheme.onPrimaryContainer }
    Card(colors = CardDefaults.cardColors(containerColor = containerColor), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("$weekdayName ${day.dayOfMonth}", fontWeight = FontWeight.Bold, color = titleColor)
                Text(day.format(dateFormatter), fontWeight = FontWeight.SemiBold, color = titleColor.copy(alpha = 0.9f))
            }
        }
    }
}

private fun formatCurrency(v: Double)       = "${formatCurrencyNumber(v)} €"
private fun formatCurrencyNumber(v: Double) = String.format(Locale.getDefault(), "%.2f", v)
private fun formatHours(v: Double)          = String.format(Locale.getDefault(), "%.1f h", v)
