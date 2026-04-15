package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.kpi

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.billingPeriodDataStore by preferencesDataStore(name = "billing_period")

data class BillingPeriod(
    val fromMillis: Long,
    val toMillis: Long
) {
    val isConfigured: Boolean get() = fromMillis > 0L && toMillis > 0L
}

object BillingPeriodStore {

    private val KEY_FROM = longPreferencesKey("BILLING_FROM_MILLIS")
    private val KEY_TO = longPreferencesKey("BILLING_TO_MILLIS")

    /**
     * Flujo "raw": puede devolver 0L si aún no está configurado.
     * Útil si quieres detectar explícitamente "no configurado".
     */
    fun periodFlowRaw(context: Context): Flow<BillingPeriod> =
        context.billingPeriodDataStore.data.map { prefs ->
            BillingPeriod(
                fromMillis = prefs[KEY_FROM] ?: 0L,
                toMillis = prefs[KEY_TO] ?: 0L
            )
        }

    /**
     * Flujo "resolved": SIEMPRE devuelve un rango válido.
     * - Si hay 0L o rango inválido, devuelve mes actual (pero NO escribe por debajo).
     *
     * Si quieres además persistir el default automáticamente, usa ensureInitialized() en tu VM.
     */
    fun periodFlowResolved(
        context: Context,
        zone: ZoneId = ZoneId.systemDefault(),
        clockDate: () -> LocalDate = { LocalDate.now(zone) }
    ): Flow<BillingPeriod> =
        periodFlowRaw(context).map { raw ->
            resolveOrDefault(raw, zone, clockDate)
        }

    /**
     * Guarda el período (normalizando: orden, límites y end inclusive).
     */
    suspend fun savePeriod(context: Context, fromMillis: Long, toMillis: Long) {
        val normalized = normalizeMillisRange(fromMillis, toMillis)
        context.billingPeriodDataStore.edit { prefs ->
            prefs[KEY_FROM] = normalized.fromMillis
            prefs[KEY_TO] = normalized.toMillis
        }
    }

    /**
     * Guarda a partir de LocalDate (inclusive).
     */
    suspend fun savePeriodDates(
        context: Context,
        fromDate: LocalDate,
        toDate: LocalDate,
        zone: ZoneId = ZoneId.systemDefault()
    ) {
        val from = localDateStartMillis(fromDate, zone)
        val to = localDateEndMillis(toDate, zone)
        savePeriod(context, from, to)
    }

    /**
     * Limpia configuración (vuelve a 0L).
     */
    suspend fun clearPeriod(context: Context) {
        context.billingPeriodDataStore.edit { prefs ->
            prefs.remove(KEY_FROM)
            prefs.remove(KEY_TO)
        }
    }

    /**
     * Lee una vez el período raw (puede venir con 0L).
     */
    suspend fun getPeriodRaw(context: Context): BillingPeriod =
        periodFlowRaw(context).first()

    /**
     * Lee una vez el período resolved (siempre válido, no escribe).
     */
    suspend fun getPeriodResolved(
        context: Context,
        zone: ZoneId = ZoneId.systemDefault(),
        clockDate: () -> LocalDate = { LocalDate.now(zone) }
    ): BillingPeriod =
        resolveOrDefault(getPeriodRaw(context), zone, clockDate)

    /**
     * ✅ PRO: asegura que en DataStore hay un período válido.
     * Si no existe o es inválido -> guarda mes actual y lo devuelve.
     *
     * Llama a esto en el init del ViewModel (una vez) y te olvidas de LaunchedEffect/0L.
     */
    suspend fun ensureInitialized(
        context: Context,
        zone: ZoneId = ZoneId.systemDefault(),
        clockDate: () -> LocalDate = { LocalDate.now(zone) }
    ): BillingPeriod {
        val raw = getPeriodRaw(context)
        val resolved = resolveOrDefault(raw, zone, clockDate)

        // Si raw era inválido/no configurado, persistimos el resolved
        if (!raw.isConfigured || raw.fromMillis > raw.toMillis) {
            savePeriod(context, resolved.fromMillis, resolved.toMillis)
        }
        return resolved
    }

    // -----------------------------
    // Helpers de fechas
    // -----------------------------

    /**
     * Conversión estricta: millis > 0.
     */
    fun millisToLocalDate(millis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate {
        require(millis > 0L) { "millisToLocalDate: millis debe ser > 0. Recibido: $millis" }
        return Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
    }

    /**
     * Conversión tolerante (por si te llega 0L): cae a "hoy".
     * Útil en UI si no quieres reventar.
     */
    fun millisToLocalDateOrToday(millis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate {
        if (millis <= 0L) return LocalDate.now(zone)
        return Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
    }

    fun localDateStartMillis(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    /**
     * Fin inclusivo del día: 23:59:59.999...
     */
    fun localDateEndMillis(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long =
        date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1L

    /**
     * Rango del mes de una fecha (inclusive).
     */
    fun monthRangeMillisFor(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): BillingPeriod {
        val start = date.with(TemporalAdjusters.firstDayOfMonth())
        val end = date.with(TemporalAdjusters.lastDayOfMonth())
        return BillingPeriod(
            fromMillis = localDateStartMillis(start, zone),
            toMillis = localDateEndMillis(end, zone)
        )
    }

    /**
     * Rango del mes actual (inclusive).
     */
    fun currentMonthRangeMillis(zone: ZoneId = ZoneId.systemDefault()): BillingPeriod =
        monthRangeMillisFor(LocalDate.now(zone), zone)

    // -----------------------------
    // Internals
    // -----------------------------

    private fun resolveOrDefault(
        raw: BillingPeriod,
        zone: ZoneId,
        clockDate: () -> LocalDate
    ): BillingPeriod {
        // Caso no configurado o inválido => default = mes actual
        if (!raw.isConfigured) return monthRangeMillisFor(clockDate(), zone)

        // Si viene invertido, lo normalizamos (no escribimos aquí)
        return normalizeMillisRange(raw.fromMillis, raw.toMillis)
    }

    /**
     * Normaliza:
     * - Si están invertidos -> los ordena
     * - Si alguno es <= 0 -> cae al mes actual (NO aquí, esto lo hace resolveOrDefault)
     */
    private fun normalizeMillisRange(fromMillis: Long, toMillis: Long): BillingPeriod {
        val a = minOf(fromMillis, toMillis)
        val b = maxOf(fromMillis, toMillis)
        return BillingPeriod(fromMillis = a, toMillis = b)
    }
}
