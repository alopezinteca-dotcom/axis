package vtsen.hashnode.dev.newemptycomposeapp.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelEntity
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelRepository

/* =========================================================
   VIEWMODEL · ACTIVITY
   ========================================================= */

class ActivityViewModel(
    private val repository: TravelRepository
) : ViewModel() {

    /* ---------- VIAJE EN CURSO ---------- */

    private val _currentTravel = MutableStateFlow<TravelEntity?>(null)
    val currentTravel: StateFlow<TravelEntity?> = _currentTravel

    /* ---------- DATOS PARA EXPORTACIÓN ---------- */

    private val _exportData = MutableStateFlow<List<TravelEntity>>(emptyList())
    val exportData: StateFlow<List<TravelEntity>> = _exportData

    init {
        loadTravelInProgress()
    }

    private fun loadTravelInProgress() {
        viewModelScope.launch {
            _currentTravel.value = repository.getTravelInProgress()
        }
    }

    /* ---------- INICIAR VIAJE ---------- */

    fun startTravel(
        origin: String,
        destination: String,
        description: String,
        kmStart: Int,
        billingExpected: Double,
        hasDiet: Boolean
    ): Boolean {
        if (origin.isBlank()) return false
        if (destination.isBlank()) return false
        if (kmStart <= 0) return false
        if (billingExpected < 0) return false

        viewModelScope.launch {
            repository.startTravel(
                origin = origin.trim(),
                destination = destination.trim(),
                description = description.trim(),
                kmStart = kmStart,
                billingExpected = billingExpected,
                hasDiet = hasDiet,
                startTimestamp = System.currentTimeMillis()
            )
            loadTravelInProgress()
        }
        return true
    }

    /* ---------- CERRAR VIAJE ---------- */

    fun closeCurrentTravel(
        kmEnd: Int,
        hoursImputed: Double
    ): Boolean {
        val travel = _currentTravel.value ?: return false
        if (kmEnd < travel.kmStart) return false
        if (hoursImputed <= 0.0) return false

        viewModelScope.launch {
            repository.closeTravel(
                travelId = travel.id,
                kmEnd = kmEnd,
                hoursImputed = hoursImputed,
                endTimestamp = System.currentTimeMillis()
            )
            _currentTravel.value = null
        }
        return true
    }

    /* ---------- PREPARAR EXPORTACIÓN ---------- */

    fun prepareExport() {
        viewModelScope.launch {
            _exportData.value = repository.exportClosedTravels()
        }
    }
}