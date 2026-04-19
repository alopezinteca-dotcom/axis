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
 * - Convierte a entidades de Room
 * - Restaura en base de datos en orden jerárquico (Viajes -> Paradas)
 */
object AxisImportCoordinator {

    enum class ImportStrategy {
        /**
         * Borra TODO y restaura desde el CSV.
         * Estrategia recomendada para instalaciones limpias o cambios de móvil.
         */
        REPLACE_ALL,

        /**
         * Upsert sin borrar (mezcla datos).
         * Útil para añadir registros nuevos sin perder el historial actual.
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

    /**
     * Proceso principal de importación desde un URI de Google Drive o local.
     */
    suspend fun importFromUnifiedCsvUri(
        context: Context,
        csvUri: Uri,
        travelRepository: TravelRepository,
        stopRepository: TravelStopRepository,
        strategy: ImportStrategy = ImportStrategy.REPLACE_ALL,
        zone: ZoneId = ZoneId.systemDefault()
    ): ImportResult = withContext(Dispatchers.IO) {

        val resolver = context.contentResolver

        // 1. Abrir stream y parsear filas
        val parsed = resolver.openInputStream(csvUri)?.use { input ->
            AxisUnifiedCsvImporter.parse(input)
        } ?: throw IllegalStateException("No se pudo acceder al archivo CSV en Drive")

        // 2. Mapear filas de VIAJES a entidades TravelEntity
        val travelEntities = parsed.travels.mapNotNull { row ->
            rowToTravelEntity(row, zone)
        }

        // Creamos un Set de IDs para validar que no importamos paradas huérfanas
        val travelIds = travelEntities.map { it.id }.toHashSet()

        // 3. Mapear filas de PARADAS a entidades TravelStopEntity
        var skipped = 0
        val stopEntities = parsed.stops.mapNotNull { row ->
            if (!travelIds.contains(row.travelId.trim())) {
                skipped++
                null
            } else {
                rowToStopEntity(row)
            }
        }

        // 4. Ejecutar persistencia según la estrategia elegida
        when (strategy) {
            ImportStrategy.REPLACE_ALL -> {
                // Limpieza total antes de restaurar
                travelRepository.deleteAll()
                stopRepository.deleteAll()

                travelRepository.upsertAll(travelEntities)
                stopRepository.upsertAll(stopEntities)
            }

            ImportStrategy.MERGE -> {
                // Insertar o actualizar sin borrar
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

    /**
     * Convierte una fila de texto en una entidad de Viaje.
     */
    private fun rowToTravelEntity(
        row: AxisUnifiedCsvImporter.TravelRow,
        zone: ZoneId
    ): TravelEntity? {
        val id = row.travelId.trim()
        if (id.isBlank()) return null

        // Reconstrucción de timestamps
        val date = runCatching { LocalDate.parse(row.travelDate.trim(), dateFmt) }.getOrNull() ?: return null
        val startTime = runCatching { LocalTime.parse(row.travelTimeStart.trim(), timeFmt) }.getOrNull() ?: LocalTime.MIDNIGHT

        val startTs = LocalDateTime.of(date, startTime)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

        val endTs = row.travelTimeEnd.trim().takeIf { it.isNotBlank() }?.let { tStr ->
            val endTime = runCatching { LocalTime.parse(tStr, timeFmt) }.getOrNull()
            endTime?.let { 
                LocalDateTime.of(date, it).atZone(zone).toInstant().toEpochMilli() 
            }
        }

        // Mapeo de campos numéricos y booleanos
        val kmStart = row.kmStart.trim().toIntOrNull() ?: 0
        val kmEnd = row.kmEnd.trim().toIntOrNull()
        val hasDiet = row.hasDiet.trim() == "1"
        val billingExpected = parseDoubleOrZero(row.billingExpected)
        val isInvoiced = row.isInvoiced.trim() == "1"

        // Estado del viaje
        val status = runCatching { TravelStatus.valueOf(row.status.trim()) }.getOrNull() 
            ?: TravelStatus.IN_PROGRESS

        // Horas y cálculos
        val hoursDraft = parseNullableDouble(row.hoursDraft)
        val hoursCalcSnapshot = parseNullableDouble(row.hoursCalcSnapshot)
        val hoursImputed = parseNullableDouble(row.hoursImputed)

        val deltaHours = if (hoursImputed != null && hoursCalcSnapshot != null) {
            hoursImputed - hoursCalcSnapshot
        } else {
            null
        }
        val hoursModified = deltaHours?.let { abs(it) > 0.01 } ?: false

        return TravelEntity(
            id = id,
            startTimestamp = startTs,
            endTimestamp = endTs,
            origin = row.origin,
            destination = row.destination,
            description = row.description,
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
            impactEuroAlejandro = null, // Se recalcula en la app si es necesario
            snapCosteKmOperativo = null,
            snapCosteDietaFija = null,
            snapPorcBenefExigidoA = null,
            snapCosteHoraAlejandro = null,
            snapCosteHoraEmpresaX = null,
            snapTarifaObjetivoY = null,
            isInvoiced = isInvoiced,
            endAddress = "" 
        )
    }

    /**
     * Convierte una fila de texto en una entidad de Parada.
     */
    private fun rowToStopEntity(row: AxisUnifiedCsvImporter.StopRow): TravelStopEntity? {
        val stopId = row.stopId.trim()
        val travelId = row.travelId.trim()
        if (stopId.isBlank() || travelId.isBlank()) return null

        val ts = row.stopTimestamp.trim().toLongOrNull() ?: return null
        val km = row.stopKmOdometer.trim().toIntOrNull()

        return TravelStopEntity(
            id = stopId,
            travelId = travelId,
            timestamp = ts,
            place = row.stopPlace,
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
