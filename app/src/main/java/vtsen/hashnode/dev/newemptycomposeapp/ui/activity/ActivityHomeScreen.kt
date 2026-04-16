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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
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
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelEntity
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelStatus
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelStopEntity
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.export.AxisUnifiedCsvExporter
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.export.ExportPreferences
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.kpi.BillingPeriod
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
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    val localeEs = remember { Locale("es", "ES") }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isExporting by remember { mutableStateOf(false) }

    val allTravels by viewModel.allTravels.collectAsStateWithLifecycle()
    val currentTravel by viewModel.currentTravel.collectAsStateWithLifecycle()
    val stopsInPeriod by viewModel.stopsInPeriod.collectAsStateWithLifecycle()

    // ===== Confirmación borrar viaje =====
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var travelToDeleteId by remember { mutableStateOf<String?>(null) }

    // ===== Editar viaje (diálogo) =====
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

    // =========================
    // 1) PERIODO (DataStore)
    // =========================
    val storedPeriod by BillingPeriodStore.periodFlowRaw(context).collectAsStateWithLifecycle(
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

    // ✅ NECESARIO para stops del periodo en ViewModel
    LaunchedEffect(fromMillis, toMillis) {
        if (fromMillis != 0L && toMillis != 0L) {
            viewModel.setStopsRange(fromMillis, toMillis)
        }
    }

    val fromDate = remember(fromMillis) {
        if (fromMillis == 0L) LocalDate.now() else BillingPeriodStore.millisToLocalDateOrToday(fromMillis, zone)
    }
    val toDate = remember(toMillis) {
        if (toMillis == 0L) LocalDate.now() else BillingPeriodStore.millisToLocalDateOrToday(toMillis, zone)
    }

    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }

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
    // 2) Festivos + Vacaciones
    // =========================
    val allHolidays by CalendarOverridesStore.holidaysFlow(context).collectAsStateWithLifecycle(initialValue = emptyList())
    val allVacations by CalendarOverridesStore.vacationsFlow(context).collectAsStateWithLifecycle(initialValue = emptyList())
    var showCalendarManager by remember { mutableStateOf(false) }

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

    val holidaysInPeriod by remember(allHolidays, fromDate, toDate) {
        derivedStateOf { allHolidays.filter { it.date in fromDate..toDate } }
    }
    val holidayByDateInPeriod by remember(holidaysInPeriod) {
        derivedStateOf { holidaysInPeriod.associateBy { it.date } }
    }
    val holidayDatesInPeriod by remember(holidaysInPeriod) {
        derivedStateOf { holidaysInPeriod.map { it.date }.toSet() }
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
    // 3) VIAJES DEL PERIODO + Timeline
    // =========================
    val periodTravels by remember(allTravels, fromMillis, toMillis) {
        derivedStateOf {
            if (fromMillis == 0L || toMillis == 0L) emptyList()
            else allTravels.filter { it.startTimestamp in fromMillis..toMillis }
        }
    }

    val daysInRange = remember(fromDate, toDate) { datesBetweenInclusive(fromDate, toDate) }

    val travelsByDay = remember(periodTravels, zone) {
        periodTravels.groupBy { t -> BillingPeriodStore.millisToLocalDateOrToday(t.startTimestamp, zone) }
    }

    val stopsCountByDay = remember(stopsInPeriod, zone) {
        stopsInPeriod
            .groupBy { s -> BillingPeriodStore.millisToLocalDateOrToday(s.timestamp, zone) }
            .mapValues { it.value.size }
    }

    // =========================
    // 4) KPI
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

    val yearStartMillis = remember { BillingPeriodStore.localDateStartMillis(LocalDate.now().with(TemporalAdjusters.firstDayOfYear()), zone) }
    val nowEndMillis = remember { BillingPeriodStore.localDateEndMillis(LocalDate.now(), zone) }
    val travelsYear by remember(allTravels, yearStartMillis, nowEndMillis) {
        derivedStateOf { allTravels.filter { it.startTimestamp in yearStartMillis..nowEndMillis } }
    }
    val totalAnualEstimado by remember(travelsYear) { derivedStateOf { travelsYear.sumOf { it.billingExpected } } }
    val pendienteFacturar by remember(allTravels) { derivedStateOf { allTravels.filter { !it.isInvoiced }.sumOf { it.billingExpected } } }

    // =========================
    // 5) Export robusto: diario fijo + cierre mensual automático
    // =========================
    fun dailyFileName(): String = "AXIS_export_actual.csv"

    fun monthKey(ym: YearMonth): String =
        String.format(Locale.getDefault(), "%04d_%02d", ym.year, ym.monthValue)

    fun monthlyFileName(ym: YearMonth): String =
        "AXIS_export_${monthKey(ym)}.csv"

    fun exportOne(folderUri: Uri, fileName: String, travels: List<TravelEntity>, stops: List<TravelStopEntity>) {
        exportUnifiedCsvIO(context, folderUri, fileName, travels, stops)
    }

    fun smartExport(folderUri: Uri) {
        if (isExporting) return
        isExporting = true

        coroutineScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // 1) Export diario fijo (siempre)
                    exportOne(folderUri, dailyFileName(), periodTravels, stopsInPeriod)

                    // 2) Cierre mensual automático (histórico)
                    val nowYm = YearMonth.now()
                    val prevYm = nowYm.minusMonths(1)
                    val prevKey = monthKey(prevYm)

                    val lastClosed = ExportPreferences.getLastClosedMonth(context)

                    if (lastClosed != prevKey) {
                        // 👉 Export mensual del mes anterior
                        // Viajes: filtrados del mes anterior (natural)
                        val prevMonthTravels = allTravels.filter { t ->
                            val d = BillingPeriodStore.millisToLocalDateOrToday(t.startTimestamp, zone)
                            d.year == prevYm.year && d.monthValue == prevYm.monthValue
                        }

                        // Stops: en este punto usamos stopsInPeriod (periodo actual).
                        // Si quieres que sean EXACTOS del mes anterior, te doy un cambio mínimo en ViewModel para pedir stopsInRange(prevMonth).
                        exportOne(folderUri, monthlyFileName(prevYm), prevMonthTravels, stopsInPeriod)

                        ExportPreferences.setLastClosedMonth(context, prevKey)
                    }
                }

                snackbarHostState.showSnackbar("✅ Export diario actualizado (+ cierre mensual si tocaba)")
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("❌ Error export: ${e.localizedMessage ?: "desconocido"}")
            } finally {
                isExporting = false
            }
        }
    }

    // =========================
    // 6) Picker de carpeta SAF forzado a DocumentsUI (para que salga Drive)
    // =========================
    val folderPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult

        val flags = result.data?.flags ?: 0
        val takeFlags = flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        runCatching { context.contentResolver.takePersistableUriPermission(uri, takeFlags) }

        ExportPreferences.saveFolderUri(context, uri)
        smartExport(uri)
    }

    fun launchFolderPickerSaf() {
        val baseIntent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }

        try {
            // Fuerza DocumentsUI (en Samsung suele hacer aparecer Drive)
            baseIntent.setPackage("com.android.documentsui")
            folderPickerLauncher.launch(baseIntent)
        } catch (_: ActivityNotFoundException) {
            baseIntent.setPackage(null)
            folderPickerLauncher.launch(baseIntent)
        } catch (_: Exception) {
            baseIntent.setPackage(null)
            folderPickerLauncher.launch(baseIntent)
        }
    }

    // =========================
    // Calendar manager
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

    // Añadir festivo
    if (showAddHoliday) {
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
    }

    // Añadir vacaciones
    if (showAddVacation) {
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
    }

    // Confirm borrar viaje
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
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") } }
        )
    }

    // Editar viaje
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
                        label = { Text("KM fin (vacío si no cerrado)") },
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

        // Pickers desde/hasta
        if (showFromPicker) {
            DatePickerDialog(
                onDismissRequest = { showFromPicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val picked = fromPickerState.selectedDateMillis
                        if (picked != null) {
                            val d = BillingPeriodStore.millisToLocalDateOrToday(picked, zone)
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
                            val d = BillingPeriodStore.millisToLocalDateOrToday(picked, zone)
                            val newFrom = if (d.isBefore(fromDate)) d else fromDate
                            saveRange(newFrom, d)
                        }
                        showToPicker = false
                    }) { Text("OK") }
                },
                dismissButton = { TextButton(onClick = { showToPicker = false }) { Text("Cancelar") } }
            ) { DatePicker(state = toPickerState) }
        }

        Row(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // IZQUIERDA KPIs
            Column(
                modifier = Modifier.weight(0.35f).fillMaxHeight().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Resumen", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

                BlockTitle("Periodo de facturación")
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
                            Button(onClick = {
                                val (mStart, mEnd) = currentMonthRangeMillis(zone)
                                coroutineScope.launch { BillingPeriodStore.savePeriod(context, mStart, mEnd) }
                            }, modifier = Modifier.weight(1f)) { Text("Este mes") }

                            Button(onClick = {
                                coroutineScope.launch { BillingPeriodStore.savePeriod(context, defaultPeriod.first, defaultPeriod.second) }
                            }, modifier = Modifier.weight(1f)) { Text("Reset") }
                        }
                    }
                }

                SectionDivider()

                BlockTitle("Teórico (Objetivos)")
                KpiLine("Días laborables", diasLaborables.toString(), "L-V menos festivos y vacaciones")
                KpiLine("Total facturar", formatCurrency(totalFacturarObjetivo), "350 € × día laborable")
                KpiLine("Horas objetivo", formatHours(horasObjetivo), "8 h × día laborable")
                KpiLine("Total km periodo", "$kmPeriodo km", "cerrados")

                SectionDivider()

                BlockTitle("Real (Periodo)")
                KpiLine("Total estimado periodo", formatCurrency(totalEstimadoPeriodo), "suma viajes del rango")
                KpiLine("Horas imputadas periodo", formatHours(horasImputadasPeriodo), "solo cerrados")
                KpiLine("Δ Horas (objetivo - imputadas)", formatHours(deltaHoras), "positivo = faltan horas")

                SectionDivider()

                BlockTitle("Anual + Tesorería")
                KpiLine("Total anual (estimado)", formatCurrency(totalAnualEstimado), "desde 1 enero")
                KpiLine("Pendiente de facturar", formatCurrency(pendienteFacturar), "todos no facturados")

                Spacer(modifier = Modifier.height(16.dp))

                // ✅ Export: si hay carpeta guardada exporta; si no, abre picker con Drive
                Button(
                    onClick = {
                        val folderUri = ExportPreferences.getFolderUri(context)
                        if (folderUri == null) {
                            launchFolderPickerSaf()
                        } else {
                            smartExport(folderUri)
                        }
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

                TextButton(onClick = { launchFolderPickerSaf() }, modifier = Modifier.fillMaxWidth()) {
                    Text("⚙️ Cambiar carpeta de exportación")
                }
            }

            // DERECHA: TIMELINE
            Column(
                modifier = Modifier.weight(0.65f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Timeline del periodo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                    items(daysInRange) { day ->
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
                                    val cleaned = vacationDescs.map { it.trim() }.filter { it.isNotBlank() }.distinct()
                                    cleaned.forEach { Text(it, color = MaterialTheme.colorScheme.onTertiaryContainer) }
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

                        val travelsToday = (travelsByDay[day] ?: emptyList())
                            .filter { it.status == TravelStatus.CLOSED }
                            .sortedBy { it.startTimestamp }

                        if (stopCount == 0 && currentDay != day && travelsToday.isEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = when {
                                        isHoliday || isWeekend -> MaterialTheme.colorScheme.errorContainer
                                        isVacation -> MaterialTheme.colorScheme.tertiaryContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(Modifier.padding(12.dp)) {
                                    val color = when {
                                        isHoliday || isWeekend -> MaterialTheme.colorScheme.error
                                        isVacation -> MaterialTheme.colorScheme.onTertiaryContainer
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                    Text("Sin viajes", color = color)
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

/* ========================= Helpers ========================= */

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
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val warning = if (travel.hoursModified) " ⚠️" else ""
    val date = BillingPeriodStore.millisToLocalDateOrToday(travel.startTimestamp, zone).format(dateFormatter)

    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth()) {
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

private fun exportUnifiedCsvIO(
    context: Context,
    folderUri: Uri,
    fileName: String,
    travels: List<TravelEntity>,
    stops: List<TravelStopEntity>
) {
    val resolver = context.contentResolver

    // borrar si existe
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
        ?: throw IllegalStateException("No se pudo crear el archivo de export en Drive")

    resolver.openOutputStream(newFileUri)?.use { stream ->
        AxisUnifiedCsvExporter.writeCsv(stream, travels, stops)
    } ?: throw IllegalStateException("No se pudo abrir OutputStream del documento (export)")
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
            val list = map.getOrPut(d) { mutableListOf() }
            list.add(v.description)
            d = d.plusDays(1)
        }
    }

    return map.mapValues { (_, v) -> v.map { it.trim() }.filter { it.isNotBlank() }.distinct() }
}

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

private fun capitalizeFirst(s: String): String =
    s.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "ES")) else it.toString() }

private fun formatCurrency(value: Double): String = "${formatCurrencyNumber(value)} €"
private fun formatCurrencyNumber(value: Double): String = String.format(Locale.getDefault(), "%.2f", value)
private fun formatHours(value: Double): String = String.format(Locale.getDefault(), "%.1f h", value)

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
        Row(
            Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("$weekdayName ${day.dayOfMonth}", fontWeight = FontWeight.Bold, color = titleColor)
                Text(day.format(dateFormatter), fontWeight = FontWeight.SemiBold, color = titleColor.copy(alpha = 0.9f))
            }
        }
    }
}
