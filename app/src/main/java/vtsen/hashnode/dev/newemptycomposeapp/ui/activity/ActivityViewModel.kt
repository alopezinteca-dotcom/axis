package vtsen.hashnode.dev.newemptycomposeapp.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.math.abs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelEntity
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelRepository
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelStatus

data class ParamSnapshots(
    val costeKmOperativo: Double,
    val costeDietaFija: Double,
    val porcBenefExigidoA: Double,
    val costeHoraAlejandro: Double,
    val costeHoraEmpresaX: Double,
    val tarifaObjetivoY: Double
)

class ActivityViewModel(
    private val repository: TravelRepository
) : ViewModel() {

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

        viewModelScope.launch {
            repository.updateHoursDraft(current.id, hoursDraft)
        }
        return true
    }

    // Compatibilidad: si alguien llama sin snapshots, usamos defaults
    fun closeCurrentTravel(
        kmEnd: Int,
        hoursImputed: Double,
        hoursCalculated: Double
    ): Boolean {
        val defaults = ParamSnapshots(
            costeKmOperativo = 0.19,
            costeDietaFija = 12.0,
            porcBenefExigidoA = 0.35,
            costeHoraAlejandro = 26.0,
            costeHoraEmpresaX = 36.65,
            tarifaObjetivoY = 42.14
        )
        return closeCurrentTravelWithSnapshots(
            kmEnd = kmEnd,
            hoursImputed = hoursImputed,
            hoursCalculated = hoursCalculated,
            snaps = defaults
        )
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
        // Intencionadamente vacío (compat)
    }
}
