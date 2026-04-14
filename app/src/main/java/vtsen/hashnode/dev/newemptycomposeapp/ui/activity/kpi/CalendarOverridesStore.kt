package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.kpi

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.calendarOverridesDataStore by preferencesDataStore(name = "calendar_overrides")

/**
 * Guardamos en DataStore como strings:
 * - Festivo: "YYYY-MM-DD|Descripcion"
 * - Vacaciones: "YYYY-MM-DD..YYYY-MM-DD|Descripcion"
 *
 * Nota: descripción puede ir vacía, pero el separador siempre existe.
 */
object CalendarOverridesStore {

    private val KEY_HOLIDAYS = stringSetPreferencesKey("HOLIDAYS")
    private val KEY_VACATIONS = stringSetPreferencesKey("VACATIONS")

    private val fmt: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    data class Holiday(
        val date: LocalDate,
        val description: String
    )

    data class Vacation(
        val from: LocalDate,
        val to: LocalDate,
        val description: String
    )

    fun holidaysFlow(context: Context): Flow<List<Holiday>> =
        context.calendarOverridesDataStore.data.map { prefs ->
            (prefs[KEY_HOLIDAYS] ?: emptySet())
                .mapNotNull { parseHoliday(it) }
                .sortedBy { it.date }
        }

    fun vacationsFlow(context: Context): Flow<List<Vacation>> =
        context.calendarOverridesDataStore.data.map { prefs ->
            (prefs[KEY_VACATIONS] ?: emptySet())
                .mapNotNull { parseVacation(it) }
                .sortedWith(compareBy({ it.from }, { it.to }))
        }

    suspend fun addHoliday(
        context: Context,
        date: LocalDate,
        description: String
    ) {
        val entry = "${date.format(fmt)}|${description.trim()}"
        context.calendarOverridesDataStore.edit { prefs ->
            val current = prefs[KEY_HOLIDAYS]?.toMutableSet() ?: mutableSetOf()
            current.add(entry)
            prefs[KEY_HOLIDAYS] = current
        }
    }

    suspend fun removeHolidayRaw(context: Context, rawEntry: String) {
        context.calendarOverridesDataStore.edit { prefs ->
            val current = prefs[KEY_HOLIDAYS]?.toMutableSet() ?: mutableSetOf()
            current.remove(rawEntry)
            prefs[KEY_HOLIDAYS] = current
        }
    }

    suspend fun addVacation(
        context: Context,
        from: LocalDate,
        to: LocalDate,
        description: String
    ) {
        val a = if (from.isAfter(to)) to else from
        val b = if (from.isAfter(to)) from else to
        val entry = "${a.format(fmt)}..${b.format(fmt)}|${description.trim()}"
        context.calendarOverridesDataStore.edit { prefs ->
            val current = prefs[KEY_VACATIONS]?.toMutableSet() ?: mutableSetOf()
            current.add(entry)
            prefs[KEY_VACATIONS] = current
        }
    }

    suspend fun removeVacationRaw(context: Context, rawEntry: String) {
        context.calendarOverridesDataStore.edit { prefs ->
            val current = prefs[KEY_VACATIONS]?.toMutableSet() ?: mutableSetOf()
            current.remove(rawEntry)
            prefs[KEY_VACATIONS] = current
        }
    }

    // ---------- parsing helpers ----------

    private fun parseHoliday(raw: String): Holiday? {
        // "YYYY-MM-DD|Desc"
        val parts = raw.split("|", limit = 2)
        if (parts.isEmpty()) return null

        return try {
            val date = LocalDate.parse(parts[0], fmt)
            val desc = if (parts.size > 1) parts[1] else ""
            Holiday(date, desc)
        } catch (_: Exception) {
            null
        }
    }

    private fun parseVacation(raw: String): Vacation? {
        // "YYYY-MM-DD..YYYY-MM-DD|Desc"
        val parts = raw.split("|", limit = 2)
        val range = parts.getOrNull(0) ?: return null
        val desc = parts.getOrNull(1) ?: ""

        val r = range.split("..", limit = 2)
        if (r.size != 2) return null

        return try {
            val from = LocalDate.parse(r[0], fmt)
            val to = LocalDate.parse(r[1], fmt)
            Vacation(from, to, desc)
        } catch (_: Exception) {
            null
        }
    }

    // Para poder borrar desde UI necesitamos el "raw entry"
    fun toRawHoliday(h: Holiday): String =
        "${h.date.format(fmt)}|${h.description}"

    fun toRawVacation(v: Vacation): String =
        "${v.from.format(fmt)}..${v.to.format(fmt)}|${v.description}"
}
