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

class ActivityViewModel(
    private val repository: TravelRepository
) : ViewModel() {

    // ✅ Todos los viajes (KPIs del mes + export)
    val allTravels: StateFlow<List<TravelEntity>> =
        repository.allTravels.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // ✅ Viaje en curso (si existe)
    val currentTravel: StateFlow<TravelEntity?> =
        repository.currentTravel.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    // ✅ Viajes cerrados (export y listas)
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

    // ✅ Guardar horas provisionales (draft) mientras está EN CURSO
    fun updateHoursDraft(hoursDraft: Double?): Boolean {
        val current = currentTravel.value ?: return false
        if (hoursDraft != null && hoursDraft < 0.0) return false

        viewModelScope.launch {
            repository.updateHoursDraft(current.id, hoursDraft)
        }
        return true
    }

    // ✅ Cerrar viaje: guarda snapshot + imputadas + flags (sin recálculo en Excel)
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

    // Compatibilidad con llamadas antiguas (ya no hace falta porque exportData es Flow->StateFlow)
    fun prepareExport() {
        // Intencionadamente vacío
    }
}
