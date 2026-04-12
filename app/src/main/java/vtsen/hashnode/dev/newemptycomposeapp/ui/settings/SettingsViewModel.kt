package vtsen.hashnode.dev.newemptycomposeapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AxisSettings> =
        repository.settingsFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AxisSettings()
        )

    fun saveAll(newSettings: AxisSettings) {
        viewModelScope.launch {
            repository.saveAll(newSettings)
        }
    }
}
