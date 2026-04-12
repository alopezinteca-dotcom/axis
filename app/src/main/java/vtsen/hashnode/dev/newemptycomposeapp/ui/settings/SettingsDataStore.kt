package vtsen.hashnode.dev.newemptycomposeapp.ui.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// ✅ Una única instancia de DataStore por proceso
private val Context.axisDataStore by preferencesDataStore(name = "axis_settings")

/**
 * Claves EXACTAS según tu columna NOMBRE_EXCEL.
 * (Así el mapping Excel/VBA queda 1:1)
 */
object SettingsKeys {
    // === Modelo A (Alejandro) ===
    val P_COSTE_KM_OPERATIVO = doublePreferencesKey("P_COSTE_KM_OPERATIVO")
    val P_COSTE_DIETA_FIJA = doublePreferencesKey("P_COSTE_DIETA_FIJA")
    val P_PORC_BENEF_EXIGIDO_A = doublePreferencesKey("P_PORC_BENEF_EXIGIDO_A")
    val P_COSTE_HORA_ALEJANDRO = doublePreferencesKey("P_COSTE_HORA_ALEJANDRO")

    // === Empresa ===
    val P_SALARIO_BRUTO_ANUAL = doublePreferencesKey("P_SALARIO_BRUTO_ANUAL")
    val P_CARGAS_EMPRESA = doublePreferencesKey("P_CARGAS_EMPRESA")
    val P_OVERHEAD_ANUAL = doublePreferencesKey("P_OVERHEAD_ANUAL")
    val P_HORAS_ANUALES = doublePreferencesKey("P_HORAS_ANUALES")
    val P_UTILIZACION = doublePreferencesKey("P_UTILIZACION")
    val P_MARGEN_EMPRESA = doublePreferencesKey("P_MARGEN_EMPRESA")
}

/**
 * Valores por defecto = los de tu hoja (imagen).
 * Esto permite que la app funcione aunque el usuario no abra Ajustes.
 */
data class AxisSettings(
    // Modelo A
    val costeKmOperativo: Double = 0.19,
    val costeDietaFija: Double = 12.00,
    val porcBenefExigidoA: Double = 0.35,
    val costeHoraAlejandro: Double = 26.00,

    // Empresa
    val salarioBrutoAnual: Double = 33000.00,
    val cargasEmpresa: Double = 0.3065,
    val overheadAnual: Double = 12000.00,
    val horasAnuales: Double = 1880.0,
    val utilizacion: Double = 0.80,
    val margenEmpresa: Double = 0.15
)

/**
 * Repository de Settings (DataStore).
 * Expone Flow<AxisSettings> y método de guardado.
 */
class SettingsRepository(
    private val context: Context
) {

    val settingsFlow: Flow<AxisSettings> =
        context.axisDataStore.data.map { prefs: Preferences ->
            AxisSettings(
                // Modelo A
                costeKmOperativo = prefs[SettingsKeys.P_COSTE_KM_OPERATIVO] ?: 0.19,
                costeDietaFija = prefs[SettingsKeys.P_COSTE_DIETA_FIJA] ?: 12.00,
                porcBenefExigidoA = prefs[SettingsKeys.P_PORC_BENEF_EXIGIDO_A] ?: 0.35,
                costeHoraAlejandro = prefs[SettingsKeys.P_COSTE_HORA_ALEJANDRO] ?: 26.00,

                // Empresa
                salarioBrutoAnual = prefs[SettingsKeys.P_SALARIO_BRUTO_ANUAL] ?: 33000.00,
                cargasEmpresa = prefs[SettingsKeys.P_CARGAS_EMPRESA] ?: 0.3065,
                overheadAnual = prefs[SettingsKeys.P_OVERHEAD_ANUAL] ?: 12000.00,
                horasAnuales = prefs[SettingsKeys.P_HORAS_ANUALES] ?: 1880.0,
                utilizacion = prefs[SettingsKeys.P_UTILIZACION] ?: 0.80,
                margenEmpresa = prefs[SettingsKeys.P_MARGEN_EMPRESA] ?: 0.15
            )
        }

    suspend fun saveAll(settings: AxisSettings) {
        context.axisDataStore.edit { prefs ->
            // Modelo A
            prefs[SettingsKeys.P_COSTE_KM_OPERATIVO] = settings.costeKmOperativo
            prefs[SettingsKeys.P_COSTE_DIETA_FIJA] = settings.costeDietaFija
            prefs[SettingsKeys.P_PORC_BENEF_EXIGIDO_A] = settings.porcBenefExigidoA
            prefs[SettingsKeys.P_COSTE_HORA_ALEJANDRO] = settings.costeHoraAlejandro

            // Empresa
            prefs[SettingsKeys.P_SALARIO_BRUTO_ANUAL] = settings.salarioBrutoAnual
            prefs[SettingsKeys.P_CARGAS_EMPRESA] = settings.cargasEmpresa
            prefs[SettingsKeys.P_OVERHEAD_ANUAL] = settings.overheadAnual
            prefs[SettingsKeys.P_HORAS_ANUALES] = settings.horasAnuales
            prefs[SettingsKeys.P_UTILIZACION] = settings.utilizacion
            prefs[SettingsKeys.P_MARGEN_EMPRESA] = settings.margenEmpresa
        }
    }
}
