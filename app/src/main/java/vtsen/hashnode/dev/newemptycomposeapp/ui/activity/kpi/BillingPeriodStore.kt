package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.kpi

import android.content.Context
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.billingPeriodDataStore by preferencesDataStore(name = "billing_period")

data class BillingPeriod(
    val fromMillis: Long,
    val toMillis: Long
)

object BillingPeriodStore {
    private val KEY_FROM = longPreferencesKey("BILLING_FROM_MILLIS")
    private val KEY_TO = longPreferencesKey("BILLING_TO_MILLIS")

    /** 0 significa "no configurado todavía" */
    fun periodFlow(context: Context): Flow<BillingPeriod> {
        return context.billingPeriodDataStore.data.map { prefs ->
            BillingPeriod(
                fromMillis = prefs[KEY_FROM] ?: 0L,
                toMillis = prefs[KEY_TO] ?: 0L
            )
        }
    }

    suspend fun savePeriod(context: Context, fromMillis: Long, toMillis: Long) {
        context.billingPeriodDataStore.edit { prefs ->
            prefs[KEY_FROM] = fromMillis
            prefs[KEY_TO] = toMillis
        }
    }

    // Helpers de fechas (para convertir entre millis y LocalDate)
    fun millisToLocalDate(millis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate {
        return Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
    }

    fun localDateStartMillis(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long {
        return date.atStartOfDay(zone).toInstant().toEpochMilli()
    }

    fun localDateEndMillis(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long {
        return date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
    }
}
