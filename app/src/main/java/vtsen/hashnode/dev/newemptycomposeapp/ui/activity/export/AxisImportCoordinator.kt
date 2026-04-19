package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.export

import android.content.Context
import android.net.Uri
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelEntity
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelRepository
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelStatus
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelStopEntity
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelStopRepository

/**
 * Coordinador de importación:
 * - Lee CSV unificado desde SAF/Drive (Uri)
 * - Parsea TRAVEL/STOP
 * - Convierte a entidades
 * - Restaura en Room en orden (travels -> stops) por FK
 */
object AxisImportCoordinator {

    enum class ImportStrategy {
        /**
         * Borra TODO y restaura desde el CSV.
         * Recomendado para "cambio de móvil" / "otra versión".
         */
        REPLACE_ALL,

        /**
         * Upsert sin borrar (mezcla).
         * Útil si quieres traer datos "nuevos" sin perder lo existente.
         */
        MERGE
    }

    data class ImportResult(
        val travelsImported: Int,
        val stopsImported: Int,
        val stopsSkippedNoTravel: Int
    )

    private val dateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    suspend fun importFromUnifiedCsvUri(
        context: Context,
        csvUri: Uri,
        travelRepository: TravelRepository,
        stopRepository: TravelStopRepository,
        strategy: ImportStrategy = ImportStrategy.REPLACE_ALL,
        zone: ZoneId = ZoneId.systemDefault()
    ): ImportResult = withContext(Dispatchers.IO) {

        val resolver = context.contentResolver

        val parsed = resolver.openInputStream(csvUri)?.use { input ->
            AxisUnifiedCsvImporter.parse(input)
        } ?: throw IllegalStateException("No se pudo abrir el CSV para importar")

        // 1) Mapear TRAVELS
        val travelEntities = parsed.travels.mapNotNull { row ->
            rowToTravelEntity(row, zone)
        }

        // Conjunto de IDs válidos para filtrar stops huérfanas
        val travelIds = travelEntities.map { it.id }.toHashSet()

        // 2) Mapear STOPS (solo si travel existe)
        var skipped = 0
        val stopEntities = parsed.stops.mapNotNull { row ->
            if (!travelIds.contains(row.travelId.trim())) {
                skipped++
                null
            } else {
                rowToStopEntity(row)
            }
        }

        // 3) Persistir en Room (orden por FK: travels primero)
        when (strategy) {
            ImportStrategy.REPLACE_ALL -> {
                travelRepository.deleteAll()
                stopRepository.deleteAll()

                travelRepository.upsertAll(travelEntities)
                stopRepository.upsertAll(stopEntities)
            }

            ImportStrategy.MERGE -> {
                travelRepository.upsertAll(travelEntities)
                stopRepository.upsertAll(stopEntities)
            }
        }

        ImportResult(
            travelsImported = travelEntities.size,
            stopsImported = stopEntities.size,
            stopsSkippedNoTravel = skipped
        )
    }

    private fun rowToTravelEntity(
        row: AxisUnifiedCsvImporter.TravelRow,
        zone: ZoneId
    ): TravelEntity? {
        val id = row.travelId.trim()
        if (id.isBlank()) return null

        // Fecha y horas (del exporter)
        val date = runCatching { LocalDate.parse(row.travelDate.trim(), dateFmt) }.getOrNull() ?: return null
        val startTime = runCatching { LocalTime.parse(row.travelTimeStart.trim(), timeFmt) }.getOrNull() ?: LocalTime.MIDNIGHT

        val startTs = LocalDateTime.of(date, startTime).atZone(zone).toInstant().toEpochMilli()

        val endTs = row.travelTimeEnd.trim().takeIf { it.isNotBlank() }?.let { tStr ->
            val endTime = runCatching { LocalTime.parse(tStr, timeFmt) }.getOrNull()
            endTime?.let { LocalDateTime.of(date, it).atZone(zone).toInstant().toEpochMilli() }
        }

        val origin = row.origin
        val destination = row.destination
        val description = row.description

        val kmStart = row.kmStart.trim().toIntOrNull() ?: 0
        val kmEnd = row.kmEnd.trim().toIntOrNull()

        val hasDiet = row.hasDiet.trim() == "1"
        val billingExpected = parseDoubleOrZero(row.billingExpected)

        val status = runCatching { TravelStatus.valueOf(row.status.trim()) }.getOrNull()
            ?: TravelStatus.IN_PROGRESS

        val hoursDraft = parseNullableDouble(row.hoursDraft)
        val hoursCalcSnapshot = parseNullableDouble(row.hoursCalcSnapshot)
        val hoursImputed = parseNullableDouble(row.hoursImputed)

        // Derivados (si vienen ambas horas)
        val deltaHours = if (hoursImputed != null && hoursCalcSnapshot != null) (hoursImputed - hoursCalcSnapshot) else null
        val hoursModified = deltaHours?.let { abs(it) > 0.01 } ?: false

        val isInvoiced = row.isInvoiced.trim() == "1"

        return TravelEntity(
            id = id,
            startTimestamp = startTs,
            endTimestamp = endTs,
            origin = origin,
            destination = destination,
            description = description,
            kmStart = kmStart,
            kmEnd = kmEnd,
            hasDiet = hasDiet,
            billingExpected = billingExpected,
            status = status,
            hoursDraft = hoursDraft,
            hoursCalculatedSnapshot = hoursCalcSnapshot,
            hoursImputed = hoursImputed,
            hoursModified = hoursModified,
            deltaHours = deltaHours,
            impactEuroAlejandro = null, // no viene en CSV
            snapCosteKmOperativo = null,
            snapCosteDietaFija = null,
            snapPorcBenefExigidoA = null,
            snapCosteHoraAlejandro = null,
            snapCosteHoraEmpresaX = null,
            snapTarifaObjetivoY = null,
            isInvoiced = isInvoiced,
            endAddress = "" // no viene en CSV
        )
    }

    private fun rowToStopEntity(row: AxisUnifiedCsvImporter.StopRow): TravelStopEntity? {
        val stopId = row.stopId.trim()
        val travelId = row.travelId.trim()
        if (stopId.isBlank() || travelId.isBlank()) return null

        val ts = row.stopTimestamp.trim().toLongOrNull() ?: return null
        val place = row.stopPlace
        val km = row.stopKmOdometer.trim().toIntOrNull()

        return TravelStopEntity(
            id = stopId,
            travelId = travelId,
            timestamp = ts,
            place = place,
            kmOdometer = km
        )
    }

    private fun parseDoubleOrZero(raw: String): Double {
        val x = raw.trim()
        if (x.isBlank()) return 0.0
        return x.replace(',', '.').toDoubleOrNull() ?: 0.0
    }

    private fun parseNullableDouble(raw: String): Double? {
        val x = raw.trim()
        if (x.isBlank()) return null
        return x.replace(',', '.').toDoubleOrNull()
    }
}
