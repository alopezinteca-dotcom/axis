package vtsen.hashnode.dev.newemptycomposeapp.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
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
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.backup.BackupManager
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.backup.BackupSnapshot
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

    // Stops del periodo (timeline) — VM usa el periodo como source of truth
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
        stopsInPeriod.groupBy { s -> BillingPeriodStore.millisToLocalDateOrToday(s.timestamp, zone) }
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
    val pendienteFacturar by remember(allTravels) { derivedStateOf { allTravels.filter { !it.isInvoiced }.sumOf { it.billingExpected } } }

    // =========================
    // 5) DRIVE (AXIS folder) + EXPORT + BACKUP + RESTORE
    // =========================
    fun axisFolderUri(): Uri? = ExportPreferences.getFolderUri(context)

    fun periodKey(): String {
        val ym = java.time.YearMonth.of(toDate.year, toDate.monthValue)
        return String.format(Locale.getDefault(), "%04d_%02d", ym.year, ym.monthValue)
    }

    fun dailyCsvName(): String = "AXIS_export_actual.csv"
    fun monthlyCsvName(key: String): String = "AXIS_export_$key.csv"

    fun todayKey(): String {
        val now = LocalDate.now()
        return String.format(Locale.getDefault(), "%04d_%02d_%02d", now.year, now.monthValue, now.dayOfMonth)
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult

        val takeFlags = (result.data?.flags ?: 0) and
            (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

        runCatching { context.contentResolver.takePersistableUriPermission(uri, takeFlags) }
        ExportPreferences.saveFolderUri(context, uri)

        coroutineScope.launch { snackbarHostState.showSnackbar("✅ Carpeta AXIS configurada") }
    }

    // ✅ Drive FIX: chooser + DocumentsUI como intent inicial
    fun launchPickAxisFolder() {
        val baseIntent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }

        val docsUi = Intent(baseIntent).apply { setPackage("com.android.documentsui") }
        val chooser = Intent.createChooser(baseIntent, "Elegir carpeta AXIS (Drive)").apply {
            // solo lo añadimos si existe
            if (docsUi.resolveActivity(context.packageManager) != null) {
                putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(docsUi))
            }
        }

        folderPickerLauncher.launch(chooser)
    }

    var showRestoreConfirm by remember { mutableStateOf(false) }
    var pendingSnapshot by remember { mutableStateOf<BackupSnapshot?>(null) }

    val restorePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) { BackupManager.readBytesFromUri(context, uri) }
                val json = withContext(Dispatchers.IO) { BackupManager.readGzipBytesToString(bytes) }
                val snapshot = parseSnapshotJson(json)
                pendingSnapshot = snapshot
                showRestoreConfirm = true
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("❌ No se pudo leer el backup: ${e.localizedMessage ?: "desconocido"}")
            }
        }
    }
    if (showRestoreConfirm && pendingSnapshot != null) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("Restaurar backup") },
            text = { Text("⚠️ Esto borrará los datos actuales y restaurará TODO desde el backup. ¿Continuar?") },
            confirmButton = {
                TextButton(onClick = {
                    val snap = pendingSnapshot!!
                    pendingSnapshot = null
                    showRestoreConfirm = false

                    coroutineScope.launch {
                        try {
                            isExporting = true
                            withContext(Dispatchers.IO) { viewModel.restoreFromSnapshot(snap) }
                            snackbarHostState.showSnackbar("✅ Restauración completa")
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("❌ Error restaurando: ${e.localizedMessage ?: "desconocido"}")
                        } finally {
                            isExporting = false
                        }
                    }
                }) { Text("Restaurar") }
            },
            dismissButton = { TextButton(onClick = { showRestoreConfirm = false }) { Text("Cancelar") } }
        )
    }

    fun findChildDocIdByName(parentTreeUri: Uri, parentDocId: String, displayName: String): String? {
        val resolver = context.contentResolver
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(parentTreeUri, parentDocId)
        resolver.query(
            childrenUri,
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null, null, null
        )?.use { c ->
            while (c.moveToNext()) {
                val docId = c.getString(0)
                val name = c.getString(1)
                if (name == displayName) return docId
            }
        }
        return null
    }

    suspend fun writeCsvIntoAxisRoot(axisTreeUri: Uri, fileName: String, travels: List<TravelEntity>, stops: List<TravelStopEntity>) {
        val resolver = context.contentResolver
        val axisDocId = DocumentsContract.getTreeDocumentId(axisTreeUri)

        val existingId = findChildDocIdByName(axisTreeUri, axisDocId, fileName)
        if (existingId != null) {
            val docUri = DocumentsContract.buildDocumentUriUsingTree(axisTreeUri, existingId)
            val ok = runCatching {
                resolver.openOutputStream(docUri, "wt")?.use { out ->
                    AxisUnifiedCsvExporter.writeCsv(out, travels, stops)
                } ?: throw IllegalStateException("No OutputStream")
            }.isSuccess
            if (ok) return
            runCatching { DocumentsContract.deleteDocument(resolver, docUri) }
        }

        val newFileUri = DocumentsContract.createDocument(resolver, axisTreeUri, "text/csv", fileName)
            ?: throw IllegalStateException("No se pudo crear $fileName")

        resolver.openOutputStream(newFileUri, "wt")?.use { out ->
            AxisUnifiedCsvExporter.writeCsv(out, travels, stops)
        } ?: throw IllegalStateException("No se pudo escribir $fileName")
    }

    suspend fun maybeDailyBackup(axisTreeUri: Uri) {
        val today = todayKey()
        val last = ExportPreferences.getLastBackupDate(context)
        if (last == today) return

        val snapshot = withContext(Dispatchers.IO) { viewModel.buildSnapshotForBackup() }
        val json = BackupManager.snapshotToJson(snapshot)
        val gz = BackupManager.writeSnapshotToGzipBytes(json)

        val backupsDirUri = BackupManager.ensureBackupsDir(context, axisTreeUri)
        BackupManager.writeBytesToDocument(
            context = context,
            parentDirUri = backupsDirUri,
            displayName = BackupManager.backupFileName(today),
            mimeType = "application/gzip",
            bytes = gz
        )

        pruneOldBackups(context, backupsDirUri, keep = 7)
        ExportPreferences.setLastBackupDate(context, today)
    }

    fun doExportAll() {
        val axis = axisFolderUri()
        if (axis == null) {
            launchPickAxisFolder()
            return
        }

        coroutineScope.launch {
            try {
                isExporting = true
                withContext(Dispatchers.IO) {
                    writeCsvIntoAxisRoot(axis, dailyCsvName(), periodTravels, stopsInPeriod)

                    val key = periodKey()
                    val lastClosed = ExportPreferences.getLastClosedKey(context)
                    if (lastClosed != key) {
                        writeCsvIntoAxisRoot(axis, monthlyCsvName(key), periodTravels, stopsInPeriod)
                        ExportPreferences.setLastClosedKey(context, key)
                    }

                    maybeDailyBackup(axis)
                }
                snackbarHostState.showSnackbar("✅ Export OK (CSV + backup si tocaba)")
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("❌ Error export: ${e.localizedMessage ?: "desconocido"}")
            } finally {
                isExporting = false
            }
        }
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

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("AXIS · Activity") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewTravelClick, containerColor = MaterialTheme.colorScheme.primary) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }
        }
    ) { padding ->

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

                Spacer(modifier = Modifier.height(12.dp))

                val axis = axisFolderUri()
                if (axis == null) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("⚠️ Carpeta AXIS no configurada", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            Text("Pulsa abajo y elige en Drive tu carpeta AXIS.", color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }

                Button(
                    onClick = { doExportAll() },
                    enabled = !isExporting,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    if (isExporting) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator(strokeWidth = 2.dp)
                            Text("PROCESANDO…", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text("📤 EXPORTAR (CSV + BACKUP)", fontWeight = FontWeight.Bold)
                    }
                }

                TextButton(onClick = { launchPickAxisFolder() }, modifier = Modifier.fillMaxWidth()) {
                    Text("⚙️ Configurar / Cambiar carpeta AXIS (Drive)")
                }

                TextButton(
                    onClick = { restorePickerLauncher.launch(arrayOf("application/gzip", "application/octet-stream", "*/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("🛟 Restaurar desde backup (.json.gz)") }
            }

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
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(12.dp)) { Text("Sin viajes") }
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
   Snapshot parse + Backup pruning
   ========================= */

private fun parseSnapshotJson(json: String): BackupSnapshot {
    val root = JSONObject(json)
    val createdAt = root.getLong("createdAtMillis")

    val bpObj = root.opt("billingPeriod")
    val billingPeriod = if (bpObj == null || bpObj == JSONObject.NULL) {
        null
    } else {
        val p = bpObj as JSONObject
        BillingPeriod(p.getLong("fromMillis"), p.getLong("toMillis"))
    }

    val holidaysArr = root.getJSONArray("holidays")
    val holidays = buildList {
        for (i in 0 until holidaysArr.length()) {
            val o = holidaysArr.getJSONObject(i)
            add(CalendarOverridesStore.Holiday(LocalDate.parse(o.getString("date")), o.optString("description", "")))
        }
    }

    val vacationsArr = root.getJSONArray("vacations")
    val vacations = buildList {
        for (i in 0 until vacationsArr.length()) {
            val o = vacationsArr.getJSONObject(i)
            add(CalendarOverridesStore.Vacation(LocalDate.parse(o.getString("from")), LocalDate.parse(o.getString("to")), o.optString("description", "")))
        }
    }

    val travelsArr = root.getJSONArray("travels")
    val travels = buildList {
        for (i in 0 until travelsArr.length()) {
            val o = travelsArr.getJSONObject(i)
            add(
                TravelEntity(
                    id = o.getString("id"),
                    startTimestamp = o.getLong("startTimestamp"),
                    endTimestamp = if (o.isNull("endTimestamp")) null else o.getLong("endTimestamp"),
                    origin = o.getString("origin"),
                    destination = o.getString("destination"),
                    description = o.getString("description"),
                    kmStart = o.getInt("kmStart"),
                    kmEnd = if (o.isNull("kmEnd")) null else o.getInt("kmEnd"),
                    hasDiet = o.getBoolean("hasDiet"),
                    billingExpected = o.getDouble("billingExpected"),
                    status = TravelStatus.valueOf(o.getString("status")),
                    hoursDraft = if (o.isNull("hoursDraft")) null else o.getDouble("hoursDraft"),
                    hoursCalculatedSnapshot = if (o.isNull("hoursCalculatedSnapshot")) null else o.getDouble("hoursCalculatedSnapshot"),
                    hoursImputed = if (o.isNull("hoursImputed")) null else o.getDouble("hoursImputed"),
                    hoursModified = o.getBoolean("hoursModified"),
                    deltaHours = if (o.isNull("deltaHours")) null else o.getDouble("deltaHours"),
                    impactEuroAlejandro = if (o.isNull("impactEuroAlejandro")) null else o.getDouble("impactEuroAlejandro"),
                    snapCosteKmOperativo = if (o.isNull("snapCosteKmOperativo")) null else o.getDouble("snapCosteKmOperativo"),
                    snapCosteDietaFija = if (o.isNull("snapCosteDietaFija")) null else o.getDouble("snapCosteDietaFija"),
                    snapPorcBenefExigidoA = if (o.isNull("snapPorcBenefExigidoA")) null else o.getDouble("snapPorcBenefExigidoA"),
                    snapCosteHoraAlejandro = if (o.isNull("snapCosteHoraAlejandro")) null else o.getDouble("snapCosteHoraAlejandro"),
                    snapCosteHoraEmpresaX = if (o.isNull("snapCosteHoraEmpresaX")) null else o.getDouble("snapCosteHoraEmpresaX"),
                    snapTarifaObjetivoY = if (o.isNull("snapTarifaObjetivoY")) null else o.getDouble("snapTarifaObjetivoY"),
                    isInvoiced = o.getBoolean("isInvoiced"),
                    endAddress = o.optString("endAddress", "")
                )
            )
        }
    }

    val stopsArr = root.getJSONArray("stops")
    val stops = buildList {
        for (i in 0 until stopsArr.length()) {
            val o = stopsArr.getJSONObject(i)
            add(
                TravelStopEntity(
                    id = o.getString("id"),
                    travelId = o.getString("travelId"),
                    timestamp = o.getLong("timestamp"),
                    place = o.getString("place"),
                    kmOdometer = if (o.isNull("kmOdometer")) null else o.getInt("kmOdometer")
                )
            )
        }
    }

    return BackupSnapshot(
        createdAtMillis = createdAt,
        billingPeriod = billingPeriod,
        holidays = holidays,
        vacations = vacations,
        travels = travels,
        stops = stops
    )
}

private fun pruneOldBackups(context: Context, backupsDirUri: Uri, keep: Int) {
    val resolver = context.contentResolver
    val dirDocId = DocumentsContract.getDocumentId(backupsDirUri)
    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(backupsDirUri, dirDocId)

    val entries = mutableListOf<Pair<String, String>>() // (name, docId)

    resolver.query(
        childrenUri,
        arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME
        ),
        null, null, null
    )?.use { c ->
        while (c.moveToNext()) {
            val docId = c.getString(0)
            val name = c.getString(1)
            if (name.startsWith("AXIS_backup_") && name.endsWith(".json.gz")) {
                entries.add(name to docId)
            }
        }
    }

    val sorted = entries.sortedByDescending { it.first } // YYYY_MM_DD => cronológico
    val toDelete = if (sorted.size > keep) sorted.drop(keep) else emptyList()

    toDelete.forEach { (_, docId) ->
        val uri = DocumentsContract.buildDocumentUriUsingTree(backupsDirUri, docId)
        runCatching { DocumentsContract.deleteDocument(resolver, uri) }
    }
}

/* =========================
   UI helpers
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

/* =========================
   Date helpers
   ========================= */

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
