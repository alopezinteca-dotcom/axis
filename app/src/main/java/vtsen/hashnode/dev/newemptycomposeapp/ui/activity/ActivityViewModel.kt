package vtsen.hashnode.dev.newemptycomposeapp.ui.activity

import.viewModelScopeimport androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.abs
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelEntity
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelRepository
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelStatus

class ActivityViewModel(
    private val repository: TravelRepository
) : ViewModel() {

    val currentTravel: StateFlow<TravelEntity?> =
        repository.currentTravel
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val exportData: StateFlow<List<TravelEntity>> =
        repository.closedTravels
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

    fun closeCurrentTravel(
        kmEnd: Int,
        hoursImputed: Double,
        hoursCalculated: Double
    ): Boolean {
        val current = currentTravel.value ?: return false

        if (kmEnd < current.kmStart) return false
        if (hoursImputed <= 0.0) return false
        if (hoursCalculated <= 0.0) return false

        val delta = hoursImputed - hoursCalculated
        val modified = abs(delta) > 0.01

        // Placeholder hasta Settings (DataStore)
        val costeHoraAlejandro = 25.0
        val impact = delta * costeHoraAlejandro

        viewModelScope.launch {
            repository.closeTravel(
                id = current.id,
                kmEnd = kmEnd,
                endTimestamp = System.currentTimeMillis(),
                hoursCalculatedSnapshot = hoursCalculated,
                hoursImputed = hoursImputed,
                hoursModified = modified,
                deltaHours = delta,
                impactEuroAlejandro = impact
            )
        }
        return true
    }
}
