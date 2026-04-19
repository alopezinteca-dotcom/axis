package vtsen.hashnode.dev.newemptycomposeapp.ui.activity

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.backup.BackupSnapshot
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelEntity
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelRepository
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelStatus
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelStopEntity
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelStopRepository
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.export.AxisBackupCoordinator
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.export.AxisImportCoordinator
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.export.AxisUnifiedCsvExporter
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.kpi.BillingPeriod
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.kpi.BillingPeriodStore
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.kpi.CalendarOverridesStore

/**
 * Estructura para snapshots económicos al cerrar viajes.
 */
data class ParamSnapshots(
    val costeKmOperativo: Double,
    val costeDietaFija: Double,
    val porcBenefExigidoA: Double,
    val costeHoraAlejandro: Double,
    val costeHoraEmpresaX: Double,
    val tarifaObjetivoY: Double
)

class ActivityViewModel(
    private val appContext: Context,
    private val repository: TravelRepository,
    private val stopRepository: TravelStopRepository
) : ViewModel() {

    private val zone: ZoneId = ZoneId.systemDefault()

    // =========================
    // 0) PERIODO DE FACTURACIÓN (DataStore)
    // =========================

    val billingPeriod: StateFlow<BillingPeriod> =
        BillingPeriodStore.periodFlowResolved(appContext, zone)
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = BillingPeriodStore.currentMonthRangeMillis(zone)
            )

    val fromDate: StateFlow<LocalDate> =
        billingPeriod
            .map { BillingPeriodStore.millisToLocalDateOrToday(it.fromMillis, zone) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LocalDate.now(zone))

    val toDate: StateFlow<LocalDate> =
        billingPeriod
            .map { BillingPeriodStore.millisToLocalDateOrToday(it.toMillis, zone) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LocalDate.now(zone))

    val periodDates: StateFlow<Pair<LocalDate, LocalDate>> =
        billingPeriod
            .map {
                val start = BillingPeriodStore.millisToLocalDateOrToday(it.fromMillis, zone)
                val end = BillingPeriodStore.millisToLocalDateOrToday(it.toMillis, zone)
                start to end
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                LocalDate.now(zone) to LocalDate.now(zone)
            )

    init {
        viewModelScope.launch {
            BillingPeriodStore.ensureInitialized(appContext, zone)
        }
    }

    fun setBillingPeriodDates(from: LocalDate, to: LocalDate) {
        val a = if (from.isAfter(to)) to else from
        val b = if (from.isAfter(to)) from else to
        viewModelScope.launch { BillingPeriodStore.savePeriodDates(appContext, a, b, zone) }
    }

    fun setBillingPeriodMillis(fromMillis: Long, toMillis: Long) {
        viewModelScope.launch { BillingPeriodStore.savePeriod(appContext, fromMillis, toMillis) }
    }

    fun resetBillingToThisMonth() {
        viewModelScope.launch {
            val p = BillingPeriodStore.currentMonthRangeMillis(zone)
            BillingPeriodStore.savePeriod(appContext, p.fromMillis, p.toMillis)
        }
    }

    fun setStopsRange(fromMillis: Long, toMillis: Long) {
        setBillingPeriodMillis(fromMillis, toMillis)
    }

    // =========================
    // 1) FLUJOS DE VIAJES
    // =========================

    val allTravels: StateFlow<List<TravelEntity>> =
        repository.allTravels.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val currentTravel: StateFlow<TravelEntity?> =
        repository.currentTravel.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    // =========================
    // 2) FLUJOS DE PARADAS
    // =========================

    val stopsForCurrentTravel: StateFlow<List<TravelStopEntity>> =
        currentTravel
            .flatMapLatest { t ->
                if (t == null) flowOf(emptyList()) else stopRepository.stopsForTravel(t.id)
            }
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    val stopsInPeriod: StateFlow<List<TravelStopEntity>> =
        billingPeriod
            .map { p -> if (p.fromMillis <= p.toMillis) p else BillingPeriod(p.toMillis, p.fromMillis) }
            .distinctUntilChanged()
            .flatMapLatest { p ->
                val from = p.fromMillis
                val to = p.toMillis
                if (from <= 0L || to <= 0L) flowOf(emptyList())
                else stopRepository.stopsInRange(from, to).distinctUntilChanged()
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    // =========================
    // 3) GESTIÓN DE PARADAS (Lógica)
    // =========================

    fun validateStopKm(kmOdometer: Int?): String? {
        val t = currentTravel.value ?: return null
        if (kmOdometer == null) return null

        val lastKm = stopsForCurrentTravel.value.lastOrNull { it.kmOdometer != null }?.kmOdometer
        return when {
            kmOdometer < t.kmStart ->
                "⚠️ El km de la parada ($kmOdometer) es menor que el km inicial (${t.kmStart})."
            lastKm != null && kmOdometer < lastKm ->
                "⚠️ El km de la parada ($kmOdometer) es menor que el km de la parada anterior ($lastKm)."
            else -> null
        }
    }

    fun addStop(place: String, kmOdometer: Int?): Boolean {
        val t = currentTravel.value ?: return false
        if (t.status != TravelStatus.IN_PROGRESS) return false
        if (place.isBlank()) return false

        viewModelScope.launch {
            stopRepository.upsert(
                TravelStopEntity(
                    travelId = t.id,
                    place = place.trim(),
                    kmOdometer = kmOdometer
                )
            )
        }
        return true
    }

    fun updateStop(stopId: String, place: String, kmOdometer: Int?): Boolean {
        val t = currentTravel.value ?: return false
        if (t.status != TravelStatus.IN_PROGRESS) return false
        
        val old = stopsForCurrentTravel.value.firstOrNull { it.id == stopId } ?: return false

        viewModelScope.launch {
            stopRepository.upsert(old.copy(place = place.trim(), kmOdometer = kmOdometer))
        }
        return true
    }

    fun deleteStop(stopId: String) {
        val t = currentTravel.value ?: return
        if (t.status != TravelStatus.IN_PROGRESS) return
        viewModelScope.launch { stopRepository.delete(stopId) }
    }

    // =========================
    // 4) GESTIÓN DE VIAJES
    // =========================

    fun startTravel(
        origin: String,
        destination: String,
        description: String,
        kmStart: Int,
        billingExpected: Double,
        hasDiet: Boolean
    ): Boolean {
        if (origin.isBlank() || destination.isBlank() || kmStart <= 0) return false

        viewModelScope.launch {
            repository.insertTravel(
                TravelEntity(
                    origin = origin.trim(),
                    destination = destination.trim(),
                    description = description.trim(),
                    kmStart = kmStart,
                    billingExpected = billingExpected,
                    hasDiet = hasDiet,
                    status = TravelStatus.IN_PROGRESS
                )
            )
        }
        return true
    }

    fun updateHoursDraft(hoursDraft: Double?): Boolean {
        val current = currentTravel.value ?: return false
        if (hoursDraft != null && hoursDraft < 0.0) return false
        viewModelScope.launch { repository.updateHoursDraft(current.id, hoursDraft) }
        return true
    }

    fun setFacturado(travelId: String, isInvoiced: Boolean) {
        viewModelScope.launch { repository.setInvoiced(travelId, isInvoiced) }
    }

    fun updateTravel(updated: TravelEntity) {
        viewModelScope.launch { repository.insertTravel(updated) }
    }

    fun deleteTravel(travelId: String) {
        viewModelScope.launch { repository.deleteTravel(travelId) }
    }

    fun closeCurrentTravel(kmEnd: Int, hoursImputed: Double, hoursCalculated: Double): Boolean {
        val defaults = ParamSnapshots(
            costeKmOperativo = 0.19, costeDietaFija = 12.0, porcBenefExigidoA = 0.35,
            costeHoraAlejandro = 26.0, costeHoraEmpresaX = 36.65, tarifaObjetivoY = 42.14
        )
        return closeCurrentTravelWithSnapshots(kmEnd, hoursImputed, hoursCalculated, defaults)
    }

    fun closeCurrentTravelWithSnapshots(
        kmEnd: Int, hoursImputed: Double, hoursCalculated: Double, snaps: ParamSnapshots
    ): Boolean {
        val current = currentTravel.value ?: return false
        if (kmEnd < current.kmStart || hoursImputed <= 0.0 || hoursCalculated <= 0.0) return false

        val delta = hoursImputed - hoursCalculated
        val modified = abs(delta) > 0.01
        val impact = delta * snaps.costeHoraAlejandro

        viewModelScope.launch {
            repository.closeTravel(
                id = current.id,
                kmEnd = kmEnd,
                endTimestamp = System.currentTimeMillis(),
                hoursCalculatedSnapshot = hoursCalculated,
                hoursImputed = hoursImputed,
                hoursModified = modified,
                deltaHours = delta,
                impactEuroAlejandro = impact,
                snapCosteKmOperativo = snaps.costeKmOperativo,
                snapCosteDietaFija = snaps.costeDietaFija,
                snapPorcBenefExigidoA = snaps.porcBenefExigidoA,
                snapCosteHoraAlejandro = snaps.costeHoraAlejandro,
                snapCosteHoraEmpresaX = snaps.costeHoraEmpresaX,
                snapTarifaObjetivoY = snaps.tarifaObjetivoY
            )
        }
        return true
    }

    // =========================
    // 5) BACKUP / RESTORE TOTAL
    // =========================

    suspend fun buildSnapshotForBackup(): BackupSnapshot {
        val bp = runCatching { BillingPeriodStore.periodFlowRaw(appContext).first() }
            .getOrElse { billingPeriod.value }

        val holidays = CalendarOverridesStore.holidaysFlow(appContext).first()
        val vacations = CalendarOverridesStore.vacationsFlow(appContext).first()
        val travels = allTravels.value
        val stops = stopRepository.allStopsOnce()

        return BackupSnapshot(
            createdAtMillis = System.currentTimeMillis(),
            billingPeriod = bp,
            holidays = holidays,
            vacations = vacations,
            travels = travels,
            stops = stops
        )
    }

    suspend fun restoreFromSnapshot(snapshot: BackupSnapshot) {
        repository.deleteAll()
        stopRepository.deleteAll()

        repository.upsertAll(snapshot.travels)
        stopRepository.upsertAll(snapshot.stops)

        // Reset calendarios
        val existingH = CalendarOverridesStore.holidaysFlow(appContext).first()
        existingH.forEach { h ->
            CalendarOverridesStore.removeHolidayRaw(appContext, CalendarOverridesStore.toRawHoliday(h))
        }
        val existingV = CalendarOverridesStore.vacationsFlow(appContext).first()
        existingV.forEach { v ->
            CalendarOverridesStore.removeVacationRaw(appContext, CalendarOverridesStore.toRawVacation(v))
        }

        snapshot.holidays.forEach { h -> CalendarOverridesStore.addHoliday(appContext, h.date, h.description) }
        snapshot.vacations.forEach { v -> CalendarOverridesStore.addVacation(appContext, v.from, v.to, v.description) }

        snapshot.billingPeriod?.let { p ->
            BillingPeriodStore.savePeriod(appContext, p.fromMillis, p.toMillis)
        }
    }

    // =========================
    // 6) EXPORT / IMPORT (Drive)
    // =========================

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportError = MutableStateFlow<String?>(null)
    val exportError: StateFlow<String?> = _exportError.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _importError = MutableStateFlow<String?>(null)
    val importError: StateFlow<String?> = _importError.asStateFlow()

    private val _lastImportResult = MutableStateFlow<AxisImportCoordinator.ImportResult?>(null)
    val lastImportResult: StateFlow<AxisImportCoordinator.ImportResult?> = _lastImportResult.asStateFlow()

    fun clearExportError() { _exportError.value = null }
    fun clearImportError() { _importError.value = null }

    // --- MODO A: Carpeta (Tree Uri) ---
    fun exportMasterAndMaybeBackupToDrive(folderUri: Uri) {
        if (_isExporting.value) return
        viewModelScope.launch {
            _isExporting.value = true
            _exportError.value = null
            try {
                withContext(Dispatchers.IO) {
                    val travelsSnapshot = allTravels.value
                    val stopsSnapshot = stopRepository.allStopsOnce()

                    AxisBackupCoordinator.exportMasterAndMaybeBackup(
                        context = appContext,
                        folderUri = folderUri,
                        allTravels = travelsSnapshot,
                        allStops = stopsSnapshot
                    )
                }
            } catch (e: Exception) {
                _exportError.value = e.localizedMessage ?: "Error desconocido"
            } finally {
                _isExporting.value = false
            }
        }
    }

    // --- MODO B: Archivo Maestro Directo (URI Individual) ---
    fun exportToMasterFileUri(masterFileUri: Uri) {
        if (_isExporting.value) return
        viewModelScope.launch {
            _isExporting.value = true
            _exportError.value = null
            try {
                withContext(Dispatchers.IO) {
                    val travelsSnapshot = allTravels.value
                    val stopsSnapshot = stopRepository.allStopsOnce()

                    val resolver = appContext.contentResolver
                    // "wt" para truncar y escribir (sobrescribir manteniendo ID)
                    resolver.openOutputStream(masterFileUri, "wt")?.use { os ->
                        AxisUnifiedCsvExporter.writeCsv(os, travelsSnapshot, stopsSnapshot)
                    } ?: throw IllegalStateException("No se pudo abrir OutputStream del maestro")
                }
            } catch (e: Exception) {
                _exportError.value = e.localizedMessage ?: "Error de escritura"
            } finally {
                _isExporting.value = false
            }
        }
    }

    // --- IMPORTACIÓN ---
    fun importFromDriveCsv(
        csvUri: Uri,
        strategy: AxisImportCoordinator.ImportStrategy = AxisImportCoordinator.ImportStrategy.REPLACE_ALL
    ) {
        if (_isImporting.value) return
        viewModelScope.launch {
            _isImporting.value = true
            _importError.value = null
            _lastImportResult.value = null
            try {
                val result = AxisImportCoordinator.importFromUnifiedCsvUri(
                    context = appContext,
                    csvUri = csvUri,
                    travelRepository = repository,
                    stopRepository = stopRepository,
                    strategy = strategy,
                    zone = zone
                )
                _lastImportResult.value = result
            } catch (e: Exception) {
                _importError.value = e.localizedMessage ?: "Error de importación"
            } finally {
                _isImporting.value = false
            }
        }
    }
}
