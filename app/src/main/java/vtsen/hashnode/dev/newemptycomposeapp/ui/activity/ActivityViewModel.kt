package vtsen.hashnode.dev.newemptycomposeapp.ui.activity

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelEntity
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelRepository
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelStatus
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelStopEntity
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelStopRepository
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.export.AxisUnifiedCsvExporter
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.kpi.BillingPeriod
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.kpi.BillingPeriodStore

data class ParamSnapshots(
    val costeKmOperativo: Double,
    val costeDietaFija: Double,
    val porcBenefExigidoA: Double,
    val costeHoraAlejandro: Double,
    val costeHoraEmpresaX: Double,
    val tarifaObjetivoY: Double
)

class ActivityViewModel(
    private val appContext: Context, // pasa applicationContext al crear el VM
    private val repository: TravelRepository,
    private val stopRepository: TravelStopRepository
) : ViewModel() {

    private val zone: ZoneId = ZoneId.systemDefault()

    // =========================
    // 0) Billing Period (DataStore) — source of truth
    // =========================

    /**
     * ✅ Fallback válido (mes actual) para evitar (0,0) y evitar pantalla vacía al abrir.
     */
    val billingPeriod: StateFlow<BillingPeriod> =
        BillingPeriodStore.periodFlowResolved(appContext, zone)
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = BillingPeriodStore.currentMonthRangeMillis(zone)
            )

    /**
     * Mantengo fromDate/toDate por compatibilidad (puedes migrar a periodDates).
     */
    val fromDate: StateFlow<LocalDate> =
        billingPeriod
            .map { BillingPeriodStore.millisToLocalDateOrToday(it.fromMillis, zone) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LocalDate.now(zone))

    val toDate: StateFlow<LocalDate> =
        billingPeriod
            .map { BillingPeriodStore.millisToLocalDateOrToday(it.toMillis, zone) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LocalDate.now(zone))

    /**
     * ✅ Pro: un solo flow con ambas fechas (menos collectors en UI).
     */
    val periodDates: StateFlow<Pair<LocalDate, LocalDate>> =
        billingPeriod
            .map {
                BillingPeriodStore.millisToLocalDateOrToday(it.fromMillis, zone) to
                    BillingPeriodStore.millisToLocalDateOrToday(it.toMillis, zone)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                LocalDate.now(zone) to LocalDate.now(zone)
            )

    init {
        // ✅ Garantiza que si estaba vacío o mal, se persiste el default.
        viewModelScope.launch {
            BillingPeriodStore.ensureInitialized(appContext, zone)
        }
    }

    fun setBillingPeriodDates(from: LocalDate, to: LocalDate) {
        // ✅ Normaliza para evitar bugs si el usuario invierte
        val a = if (from.isAfter(to)) to else from
        val b = if (from.isAfter(to)) from else to

        viewModelScope.launch {
            BillingPeriodStore.savePeriodDates(appContext, a, b, zone)
        }
    }

    fun setBillingPeriodMillis(fromMillis: Long, toMillis: Long) {
        viewModelScope.launch {
            BillingPeriodStore.savePeriod(appContext, fromMillis, toMillis)
        }
    }

    fun resetBillingToThisMonth() {
        viewModelScope.launch {
            val p = BillingPeriodStore.currentMonthRangeMillis(zone)
            BillingPeriodStore.savePeriod(appContext, p.fromMillis, p.toMillis)
        }
    }

    // =========================
    // 1) Travels flows
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

    val exportData: StateFlow<List<TravelEntity>> =
        repository.closedTravels.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // =========================
    // 2) Stops del viaje en curso
    // =========================

    val stopsForCurrentTravel: StateFlow<List<TravelStopEntity>> =
        currentTravel.flatMapLatest { t ->
            if (t == null) flowOf(emptyList()) else stopRepository.stopsForTravel(t.id)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // =========================
    // 3) Stops del periodo (timeline) — PRO: normalizado, distinct, sin debounce
    // =========================

    val stopsInPeriod: StateFlow<List<TravelStopEntity>> =
        billingPeriod
            .map { p ->
                // defensivo: por si viniese invertido
                if (p.fromMillis <= p.toMillis) p else BillingPeriod(p.toMillis, p.fromMillis)
            }
            .distinctUntilChanged()
            .flatMapLatest { p ->
                val from = p.fromMillis
                val to = p.toMillis
                if (from <= 0L || to <= 0L) {
                    flowOf(emptyList())
                } else {
                    stopRepository
                        .stopsInRange(from, to)
                        .distinctUntilChanged()
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    /**
     * ✅ Compatibilidad: antes setStopsRange mutaba un range interno.
     * Ahora: escribe periodo en DataStore (source of truth).
     */
    fun setStopsRange(fromMillis: Long, toMillis: Long) {
        setBillingPeriodMillis(fromMillis, toMillis)
    }

    // =========================
    // 4) PARADAS (solo EN CURSO)
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
        if (kmOdometer != null && kmOdometer < 0) return false

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
        if (place.isBlank()) return false
        if (kmOdometer != null && kmOdometer < 0) return false

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
    // 5) VIAJES
    // =========================

    fun startTravel(
        origin: String,
        destination: String,
        description: String,
        kmStart: Int,
        billingExpected: Double,
        hasDiet: Boolean
    ): Boolean {
        if (origin.isBlank() || destination.isBlank()) return false
        if (kmStart <= 0) return false
        if (billingExpected < 0) return false

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

    /**
     * ✅ Seguro: usa tu upsert REPLACE (no depende de @Update).
     */
    fun updateTravel(updated: TravelEntity) {
        viewModelScope.launch {
            repository.insertTravel(updated)
        }
    }

    fun deleteTravel(travelId: String) {
        viewModelScope.launch { repository.deleteTravel(travelId) }
    }

    fun updateKmStart(kmStart: Int): Boolean {
        val current = currentTravel.value ?: return false
        if (kmStart <= 0) return false
        viewModelScope.launch { repository.updateKmStart(current.id, kmStart) }
        return true
    }

    fun closeCurrentTravel(kmEnd: Int, hoursImputed: Double, hoursCalculated: Double): Boolean {
        val defaults = ParamSnapshots(
            costeKmOperativo = 0.19,
            costeDietaFija = 12.0,
            porcBenefExigidoA = 0.35,
            costeHoraAlejandro = 26.0,
            costeHoraEmpresaX = 36.65,
            tarifaObjetivoY = 42.14
        )
        return closeCurrentTravelWithSnapshots(kmEnd, hoursImputed, hoursCalculated, defaults)
    }

    fun closeCurrentTravelWithSnapshots(
        kmEnd: Int,
        hoursImputed: Double,
        hoursCalculated: Double,
        snaps: ParamSnapshots
    ): Boolean {
        val current = currentTravel.value ?: return false

        if (kmEnd < current.kmStart) return false
        if (hoursImputed <= 0.0) return false
        if (hoursCalculated <= 0.0) return false

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

    fun prepareExport() {
        // compat
    }

    // =========================
    // 6) EXPORT (FUNCIONAL)
    // =========================

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportError = MutableStateFlow<String?>(null)
    val exportError: StateFlow<String?> = _exportError.asStateFlow()

    /**
     * Exporta CSV al folderUri (SAF / Drive). Borra si existe.
     * Mantén tu fileName determinista desde UI.
     */
    fun exportUnifiedCsv(folderUri: Uri, fileName: String) {
        if (_isExporting.value) return

        val travelsSnapshot = allTravels.value
        val stopsSnapshot = stopsInPeriod.value

        viewModelScope.launch {
            _isExporting.value = true
            _exportError.value = null
            try {
                withContext(Dispatchers.IO) {
                    exportUnifiedCsvIO(
                        context = appContext,
                        folderUri = folderUri,
                        fileName = fileName,
                        travels = travelsSnapshot,
                        stops = stopsSnapshot
                    )
                }
            } catch (e: Exception) {
                _exportError.value = e.localizedMessage ?: "Error desconocido"
            } finally {
                _isExporting.value = false
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
            DocumentsContract.buildChildDocumentsUriUsingTree(
                folderUri,
                DocumentsContract.getTreeDocumentId(folderUri)
            ),
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME
            ),
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
}
