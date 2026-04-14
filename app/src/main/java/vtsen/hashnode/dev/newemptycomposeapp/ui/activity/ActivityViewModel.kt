package vtsen.hashnode.dev.newemptycomposeapp.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelEntity
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelRepository
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelStatus
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelStopEntity
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelStopRepository

data class ParamSnapshots(
    val costeKmOperativo: Double,
    val costeDietaFija: Double,
    val porcBenefExigidoA: Double,
    val costeHoraAlejandro: Double,
    val costeHoraEmpresaX: Double,
    val tarifaObjetivoY: Double
)

class ActivityViewModel(
    private val repository: TravelRepository,
    private val stopRepository: TravelStopRepository
) : ViewModel() {

    val allTravels: StateFlow<List<TravelEntity>> =
        repository.allTravels.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val currentTravel: StateFlow<TravelEntity?> =
        repository.currentTravel.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val exportData: StateFlow<List<TravelEntity>> =
        repository.closedTravels.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // -------- Stops del viaje en curso --------
    val stopsForCurrentTravel: StateFlow<List<TravelStopEntity>> =
        currentTravel.flatMapLatest { t ->
            if (t == null) flowOf(emptyList()) else stopRepository.stopsForTravel(t.id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // -------- Stops del periodo (para timeline por día) --------
    private val stopRange = MutableStateFlow(0L to 0L)

    val stopsInPeriod: StateFlow<List<TravelStopEntity>> =
        stopRange.flatMapLatest { (from, to) ->
            if (from == 0L || to == 0L) flowOf(emptyList())
            else stopRepository.stopsInRange(from, to)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setStopsRange(fromMillis: Long, toMillis: Long) {
        stopRange.value = fromMillis to toMillis
    }

    /**
     * Devuelve un aviso si el km es sospechoso, pero NO bloquea.
     */
    fun validateStopKm(kmOdometer: Int?): String? {
        val t = currentTravel.value ?: return null
        if (kmOdometer == null) return null

        val lastKm = stopsForCurrentTravel.value.lastOrNull { it.kmOdometer != null }?.kmOdometer
        return when {
            kmOdometer < t.kmStart ->
                "⚠️ El km de la parada ($kmOdometer) es menor que el km inicial del viaje (${t.kmStart})."
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
            stopRepository.upsert(
                old.copy(
                    place = place.trim(),
                    kmOdometer = kmOdometer
                )
            )
        }
        return true
    }

    fun deleteStop(stopId: String) {
        val t = currentTravel.value ?: return
        if (t.status != TravelStatus.IN_PROGRESS) return
        viewModelScope.launch { stopRepository.delete(stopId) }
    }

    // -------- Viajes (lo tuyo) --------
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
}
