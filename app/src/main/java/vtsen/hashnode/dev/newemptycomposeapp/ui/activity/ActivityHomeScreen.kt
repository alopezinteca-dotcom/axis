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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.DayOfWeek
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
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.kpi.BillingPeriod
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.kpi.BillingPeriodStore
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.kpi.CalendarManagementDialog
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.kpi.CalendarOverridesStore
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
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isExporting by remember { mutableStateOf(false) }

    
    val allTravels by viewModel.allTravels.collectAsStateWithLifecycle()
    val currentTravel by viewModel.currentTravel.collectAsStateWithLifecycle()
    val stopsInPeriod by viewModel.stopsInPeriod.collectAsStateWithLifecycle()


    // =========================
    // 1) PERIODO (DataStore)
    // =========================
    val storedPeriod by BillingPeriodStore.periodFlow(context).collectAsStateWithLifecycle(
        initialValue = BillingPeriod(0L, 0L)
    )

    val defaultPeriod = remember { currentMonthRangeMillis(zone) }

    var fromMillis by remember { mutableStateOf(0L) }
    var toMillis by remember { mutableStateOf(0L) }

    LaunchedEffect(storedPeriod.fromMillis, storedPeriod.toMillis) {
        if (storedPeriod.fromMillis == 0L || storedPeriod.toMillis == 0L) {
            BillingPeriodStore.savePeriod(context, defaultPeriod.first, defaultPeriod.second)
            fromMillis = defaultPeriod.first
            toMillis = defaultPeriod.second
        } else {
            fromMillis = storedPeriod.fromMillis
            toMillis = storedPeriod.toMillis
        }
    }

LaunchedEffect(fromMillis, toMillis) {
    if (fromMillis != 0L && toMillis != 0L) {
        viewModel.setStopsRange(fromMillis, toMillis)
    }
}
    
    val fromDate = remember(fromMillis) {
        if (fromMillis == 0L) LocalDate.now()
        else BillingPeriodStore.millisToLocalDate(fromMillis, zone)
    }
    val toDate = remember(toMillis) {
        if (toMillis == 0L) LocalDate.now()
        else BillingPeriodStore.millisToLocalDate(toMillis, zone)
    }

    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var travelToDeleteId by remember { mutableStateOf<String?>(null) }

    val fromPickerState = rememberDatePickerState(
        initialSelectedDateMillis = BillingPeriodStore.localDateStartMillis(fromDate, zone)
    )
    val toPickerState = rememberDatePickerState(
        initialSelectedDateMillis = BillingPeriodStore.localDateStartMillis(toDate, zone)
    )

    fun saveRange(newFrom: LocalDate, newTo: LocalDate) {
        val f = BillingPeriodStore.localDateStartMillis(newFrom, zone)
        val t = BillingPeriodStore.localDateEndMillis(newTo, zone)
        coroutineScope.launch { BillingPeriodStore.savePeriod(context, f, t) }
    }

    // =========================
    // 2) Festivos + Vacaciones (manual)
    // =========================
    val allHolidays by CalendarOverridesStore.holidaysFlow(context).collectAsStateWithLifecycle(initialValue = emptyList())
    val allVacations by CalendarOverridesStore.vacationsFlow(context).collectAsStateWithLifecycle(initialValue = emptyList())

    var showCalendarManager by remember { mutableStateOf(false) }

    // Dialogs añadir festivo/vacaciones
    var showAddHoliday by remember { mutableStateOf(false) }
    var showAddVacation by remember { mutableStateOf(false) }
    var holidayDesc by remember { mutableStateOf("") }
    var vacationDesc by remember { mutableStateOf("") }

    val holidayPickerState = rememberDatePickerState(
        initialSelectedDateMillis = BillingPeriodStore.localDateStartMillis(LocalDate.now(), zone)
    )
    val vacFromPickerState = rememberDatePickerState(
        initialSelectedDateMillis = BillingPeriodStore.localDateStartMillis(LocalDate.now(), zone)
    )
    val vacToPickerState = rememberDatePickerState(
        initialSelectedDateMillis = BillingPeriodStore.localDateStartMillis(LocalDate.now(), zone)
    )

    // =========================
    // 3) VIAJES FILTRADOS POR PERIODO
    // =========================
    val periodTravels by remember(allTravels, fromMillis, toMillis) {
        derivedStateOf {
            if (fromMillis == 0L || toMillis == 0L) emptyList()
            else allTravels.filter { it.startTimestamp in fromMillis..toMillis }
        }
    }
// =========================
// 3.1) DÍAS + AGRUPACIONES PARA TIMELINE (por día)
// =========================
val stopsInPeriod by viewModel.stopsInPeriod.collectAsStateWithLifecycle()

val daysInRange = remember(fromDate, toDate) {
    datesBetweenInclusive(fromDate, toDate) // helper al final del archivo
}

// Viajes agrupados por día
val travelsByDay = remember(periodTravels, zone) {
    periodTravels.groupBy { t ->
        BillingPeriodStore.millisToLocalDate(t.startTimestamp, zone)
    }
}

// Paradas agrupadas por día (solo contador)
val stopsCountByDay = remember(stopsInPeriod, zone) {
    stopsInPeriod
        .groupBy { s -> BillingPeriodStore.millisToLocalDate(s.timestamp, zone) }
        .mapValues { it.value.size }
}
 
    // =========================
    // 4) KPI REAL PERIODO
    // =========================
    val totalEstimadoPeriodo by remember(periodTravels) { derivedStateOf { periodTravels.sumOf { it.billingExpected } } }

    val horasImputadasPeriodo by remember(periodTravels) {
        derivedStateOf {
            periodTravels
                .filter { it.status == TravelStatus.CLOSED }
                .sumOf { it.hoursImputed ?: 0.0 }
        }
    }

    val kmPeriodo by remember(periodTravels) {
        derivedStateOf {
            periodTravels
                .filter { it.status == TravelStatus.CLOSED }
                .sumOf { t -> ((t.kmEnd ?: t.kmStart) - t.kmStart).coerceAtLeast(0) }
        }
    }

    val holidayDatesInPeriod by remember(allHolidays, fromDate, toDate) {
        derivedStateOf { allHolidays.filter { it.date in fromDate..toDate }.map { it.date }.toSet() }
    }
    val vacationDatesInPeriod by remember(allVacations, fromDate, toDate) {
        derivedStateOf { expandVacationDates(allVacations, fromDate, toDate) }
    }

    val diasLaborablesReales by remember(fromDate, toDate, holidayDatesInPeriod, vacationDatesInPeriod) {
        derivedStateOf {
            countWeekdaysInclusive(fromDate, toDate) -
                countWeekdaysInSet(fromDate, toDate, holidayDatesInPeriod) -
                countWeekdaysInSet(fromDate, toDate, vacationDatesInPeriod)
        }
    }
    val diasLaborables = if (diasLaborablesReales < 0) 0 else diasLaborablesReales
    val totalFacturarObjetivo by remember(diasLaborables) { derivedStateOf { 350.0 * diasLaborables } }
    val horasObjetivo by remember(diasLaborables) { derivedStateOf { 8.0 * diasLaborables } }
    val deltaHoras by remember(horasObjetivo, horasImputadasPeriodo) { derivedStateOf { horasObjetivo - horasImputadasPeriodo } }

    val yearStartMillis = remember {
        BillingPeriodStore.localDateStartMillis(LocalDate.now().with(TemporalAdjusters.firstDayOfYear()), zone)
    }
    val nowEndMillis = remember {
        BillingPeriodStore.localDateEndMillis(LocalDate.now(), zone)
    }
    val travelsYear by remember(allTravels, yearStartMillis, nowEndMillis) {
        derivedStateOf { allTravels.filter { it.startTimestamp in yearStartMillis..nowEndMillis } }
    }
    val totalAnualEstimado by remember(travelsYear) { derivedStateOf { travelsYear.sumOf { it.billingExpected } } }

    val pendienteFacturar by remember(allTravels) {
        derivedStateOf { allTravels.filter { !it.isInvoiced }.sumOf { it.billingExpected } }
    }

    // =========================
    // 5) Export CSV
    // =========================
    fun triggerExport(folderUri: Uri) {
        if (isExporting) return
        isExporting = true
        val fileName = ExportUtils.currentMonthFileName()
        coroutineScope.launch {
            try {
                withContext(Dispatchers.IO) { exportCsvIO(context, folderUri, periodTravels) }
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

    // =========================
    // UI: Diálogo “Gestión calendario”
    // =========================
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

    // =========================
    // Scaffold con TopAppBar + 📅
    // =========================
if (showDeleteConfirm && travelToDeleteId != null) {
    AlertDialog(
        onDismissRequest = { showDeleteConfirm = false },
        title = { Text("Eliminar viaje") },
        text = { Text("⚠️ Se eliminará el viaje y todas sus paradas. ¿Continuar?") },
        confirmButton = {
            TextButton(onClick = {
                viewModel.deleteTravel(travelToDeleteId!!)
                travelToDeleteId = null
                showDeleteConfirm = false
            }) { Text("Eliminar") }
        },
        dismissButton = {
            TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") }
        }
    )
}    
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("AXIS · Activity") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    TextButton(onClick = { showCalendarManager = true }) {
                        Text("📅", fontSize = MaterialTheme.typography.titleLarge.fontSize)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewTravelClick,
                containerColor = MaterialTheme.colorScheme.primary
            ) { Text("+", style = MaterialTheme.typography.titleLarge) }
        }
    ) { padding ->

        // Pickers desde/hasta
        if (showFromPicker) {
            DatePickerDialog(
                onDismissRequest = { showFromPicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val picked = fromPickerState.selectedDateMillis
                        if (picked != null) {
                            val d = BillingPeriodStore.millisToLocalDate(picked, zone)
                            val newTo = if (d.isAfter(toDate)) d else toDate
                            saveRange(d, newTo)
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
                            val d = BillingPeriodStore.millisToLocalDate(picked, zone)
                            val newFrom = if (d.isBefore(fromDate)) d else fromDate
                            saveRange(newFrom, d)
                        }
                        showToPicker = false
                    }) { Text("OK") }
                },
                dismissButton = { TextButton(onClick = { showToPicker = false }) { Text("Cancelar") } }
            ) { DatePicker(state = toPickerState) }
        }

        // Añadir festivo
        if (showAddHoliday) {
            DatePickerDialog(
                onDismissRequest = { showAddHoliday = false },
                confirmButton = {
                    TextButton(onClick = {
                        val picked = holidayPickerState.selectedDateMillis
                        if (picked != null) {
                            val d = BillingPeriodStore.millisToLocalDate(picked, zone)
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
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DatePicker(state = holidayPickerState)
                    OutlinedTextField(
                        value = holidayDesc,
                        onValueChange = { holidayDesc = it },
                        label = { Text("Descripción (festivo)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Añadir vacaciones
        if (showAddVacation) {
            DatePickerDialog(
                onDismissRequest = { showAddVacation = false },
                confirmButton = {
                    TextButton(onClick = {
                        val aMillis = vacFromPickerState.selectedDateMillis
                        val bMillis = vacToPickerState.selectedDateMillis
                        if (aMillis != null && bMillis != null) {
                            val a = BillingPeriodStore.millisToLocalDate(aMillis, zone)
                            val b = BillingPeriodStore.millisToLocalDate(bMillis, zone)
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
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Desde", fontWeight = FontWeight.SemiBold)
                    DatePicker(state = vacFromPickerState)
                    Text("Hasta", fontWeight = FontWeight.SemiBold)
                    DatePicker(state = vacToPickerState)
                    OutlinedTextField(
                        value = vacationDesc,
                        onValueChange = { vacationDesc = it },
                        label = { Text("Descripción (vacaciones)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Layout principal
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // IZQUIERDA KPIs
            Column(
                modifier = Modifier.weight(0.35f).fillMaxHeight().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Resumen", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

                BlockTitle("Periodo de facturación")
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
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
                            Button(
                                onClick = {
                                    val (mStart, mEnd) = currentMonthRangeMillis(zone)
                                    coroutineScope.launch { BillingPeriodStore.savePeriod(context, mStart, mEnd) }
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Este mes") }
                            Button(
                                onClick = {
                                    coroutineScope.launch { BillingPeriodStore.savePeriod(context, defaultPeriod.first, defaultPeriod.second) }
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Reset") }
                        }
                    }
                }

                SectionDivider()

                BlockTitle("Teórico (Objetivos)")
                KpiLine("Días laborables", diasLaborables.toString(), "L-V menos festivos y vacaciones")
                KpiLine("Total facturar", formatCurrency(totalFacturarObjetivo), "350 € × día laborable")
                KpiLine("Horas objetivo", formatHours(horasObjetivo), "8 h × día laborable")
                KpiLine("Total km periodo", "$kmPeriodo km", "cerrados (por ahora)")

                SectionDivider()

                BlockTitle("Real (Periodo)")
                KpiLine("Total estimado periodo", formatCurrency(totalEstimadoPeriodo), "suma de viajes del rango")
                KpiLine("Horas imputadas periodo", formatHours(horasImputadasPeriodo), "solo viajes cerrados")
                KpiLine("Δ Horas (objetivo - imputadas)", formatHours(deltaHoras), "positivo = faltan horas")

                SectionDivider()

                BlockTitle("Anual + Tesorería")
                KpiLine("Total anual (estimado)", formatCurrency(totalAnualEstimado), "desde 1 de enero")
                KpiLine("Pendiente de facturar", formatCurrency(pendienteFacturar), "todos los viajes no facturados")

                Spacer(modifier = Modifier.height(16.dp))

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

           // DERECHA: TIMELINE POR DÍA (ASC)
Column(
    modifier = Modifier.weight(0.65f).fillMaxHeight(),
    verticalArrangement = Arrangement.spacedBy(12.dp)
) {
    Text("Timeline del periodo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(daysInRange) { day ->

            // Cabecera del día
            DayHeader(day = day, dateFormatter = dateFormatter)

            // 📍 Solo número de paradas del día
            val stopCount = stopsCountByDay[day] ?: 0
            if (stopCount > 0) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📍 $stopCount", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // EN CURSO si pertenece a este día
            val current = currentTravel
            val currentDay = current?.startTimestamp?.let { BillingPeriodStore.millisToLocalDate(it, zone) }
            if (current != null && current.status == TravelStatus.IN_PROGRESS && currentDay == day) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCurrentTravelClick() }
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("🟢 EN CURSO (tocar para continuar)", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${current.origin} → ${current.destination}", style = MaterialTheme.typography.titleMedium)
                        Text("KM inicio: ${current.kmStart} | Draft: ${current.hoursDraft ?: 0.0}h")
                    }
                }
            }

            // Viajes cerrados del día (orden ascendente)
            val travelsToday = (travelsByDay[day] ?: emptyList())
                .filter { it.status == TravelStatus.CLOSED }
                .sortedBy { it.startTimestamp }

            // Día vacío
            if (stopCount == 0 && (currentDay != day) && travelsToday.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(12.dp)) {
                        Text("Sin viajes", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                travelsToday.forEach { t ->
                    TravelRowCard(
    travel = t,
    zone = zone,
    dateFormatter = dateFormatter,
    onToggleInvoiced = { checked -> viewModel.setFacturado(t.id, checked) },
    onClick = { onEditTravelClick(t.id) },
    onDelete = {
        travelToDeleteId = t.id
        showDeleteConfirm = true
    }
)


            Spacer(Modifier.height(6.dp))
        }
    }
}

/* =========================
   Helpers (KPI + export)
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
private fun KpiLine(title: String, value: String, subtitle: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(10.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TravelRowCard(
    travel: TravelEntity,
    zone: ZoneId,
    dateFormatter: DateTimeFormatter,
    onToggleInvoiced: (Boolean) -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val warning = if (travel.hoursModified) " ⚠️" else ""
    val date = BillingPeriodStore.millisToLocalDate(travel.startTimestamp, zone).format(dateFormatter)

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("$date · ${travel.origin} → ${travel.destination}$warning", fontWeight = FontWeight.SemiBold)
                    Text("Ref: ${travel.description}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Fact.", style = MaterialTheme.typography.labelMedium)
                    Checkbox(
                        checked = travel.isInvoiced,
                        onCheckedChange = { onToggleInvoiced(it) },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                    )
                    TextButton(onClick = onDelete) { Text("🗑️") }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Horas imputadas: ${travel.hoursImputed?.let { formatHours(it) } ?: "—"}")
                Text("€ ${formatCurrencyNumber(travel.billingExpected)}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun exportCsvIO(context: Context, folderUri: Uri, travels: List<TravelEntity>) {
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

private fun countWeekdaysInclusive(from: LocalDate, to: LocalDate): Int {
    if (to.isBefore(from)) return 0
    var d = from
    var count = 0
    while (!d.isAfter(to)) {
        val dow = d.dayOfWeek
        if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) count++
        d = d.plusDays(1)
    }
    return count
}

private fun countWeekdaysInSet(from: LocalDate, to: LocalDate, dates: Set<LocalDate>): Int {
    if (dates.isEmpty()) return 0
    return dates.count { it in from..to && it.dayOfWeek != DayOfWeek.SATURDAY && it.dayOfWeek != DayOfWeek.SUNDAY }
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

private fun formatCurrency(value: Double): String = "${formatCurrencyNumber(value)} €"
private fun formatCurrencyNumber(value: Double): String = String.format(Locale.getDefault(), "%.2f", value)
private fun formatHours(value: Double): String = String.format(Locale.getDefault(), "%.1f h", value)
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

@Composable
private fun DayHeader(day: LocalDate, dateFormatter: DateTimeFormatter) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(day.format(dateFormatter), fontWeight = FontWeight.Bold)
            Text("Día ${day.dayOfMonth}", fontWeight = FontWeight.SemiBold)
        }
    }
}
