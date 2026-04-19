package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.export

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.lang.StringBuilder

/**
 * Importador del CSV unificado (TRAVEL/STOP) generado por AxisUnifiedCsvExporter.
 *
 * - Soporta campos con comillas y comas dentro (CSV estándar).
 * - Ignora línea opcional "sep=," (útil para Excel a veces).
 * - No depende de Room ni de entidades: devuelve estructuras "row" listas para mapear.
 */
object AxisUnifiedCsvImporter {

    data class UnifiedCsvData(
        val travels: List<TravelRow>,
        val stops: List<StopRow>
    )

    data class TravelRow(
        val travelId: String,
        val travelDate: String,          // yyyy-MM-dd
        val travelTimeStart: String,     // HH:mm
        val travelTimeEnd: String,       // HH:mm or ""
        val origin: String,
        val destination: String,
        val description: String,
        val status: String,              // enum name
        val kmStart: String,
        val kmEnd: String,
        val kmTotal: String,
        val hasDiet: String,             // "1"/"0"
        val billingExpected: String,     // "12.34"
        val isInvoiced: String,          // "1"/"0"
        val hoursCalcSnapshot: String,
        val hoursImputed: String,
        val hoursDraft: String
    )

    data class StopRow(
        val stopId: String,
        val travelId: String,
        val travelDate: String,       // yyyy-MM-dd
        val stopTimestamp: String,    // epoch millis as string
        val stopTime: String,         // HH:mm
        val stopPlace: String,
        val stopKmOdometer: String
    )

    /**
     * Lee y parsea el CSV unificado desde un InputStream.
     */
    fun parse(
        input: InputStream,
        charset: Charset = Charsets.UTF_8
    ): UnifiedCsvData {
        val travels = ArrayList<TravelRow>()
        val stops = ArrayList<StopRow>()

        BufferedReader(InputStreamReader(input, charset)).use { br ->
            var headerRead = false

            br.lineSequence().forEach { rawLine ->
                val line = rawLine.trim()
                if (line.isBlank()) {
                    return@forEach
                }

                // Excel hack: "sep=,"
                if (line.startsWith("sep=", ignoreCase = true)) {
                    return@forEach
                }

                // La primera línea no vacía debe ser cabecera
                if (!headerRead) {
                    headerRead = true
                    return@forEach
                }

                val cols = parseCsvLine(line)
                if (cols.isEmpty()) {
                    return@forEach
                }

                val rowType = cols.getOrNull(0)?.trim().orEmpty()

                when (rowType) {
                    "TRAVEL" -> {
                        val t = TravelRow(
                            travelId = cols.getOrNull(1).orEmpty(),
                            travelDate = cols.getOrNull(2).orEmpty(),
                            travelTimeStart = cols.getOrNull(3).orEmpty(),
                            travelTimeEnd = cols.getOrNull(4).orEmpty(),
                            origin = cols.getOrNull(5).orEmpty(),
                            destination = cols.getOrNull(6).orEmpty(),
                            description = cols.getOrNull(7).orEmpty(),
                            status = cols.getOrNull(8).orEmpty(),
                            kmStart = cols.getOrNull(9).orEmpty(),
                            kmEnd = cols.getOrNull(10).orEmpty(),
                            kmTotal = cols.getOrNull(11).orEmpty(),
                            hasDiet = cols.getOrNull(12).orEmpty(),
                            billingExpected = cols.getOrNull(13).orEmpty(),
                            isInvoiced = cols.getOrNull(14).orEmpty(),
                            hoursCalcSnapshot = cols.getOrNull(15).orEmpty(),
                            hoursImputed = cols.getOrNull(16).orEmpty(),
                            hoursDraft = cols.getOrNull(17).orEmpty()
                        )
                        if (t.travelId.isNotBlank()) {
                            travels.add(t)
                        }
                    }

                    "STOP" -> {
                        val s = StopRow(
                            stopId = cols.getOrNull(18).orEmpty(),
                            travelId = cols.getOrNull(1).orEmpty(),
                            travelDate = cols.getOrNull(2).orEmpty(),
                            stopTimestamp = cols.getOrNull(19).orEmpty(),
                            stopTime = cols.getOrNull(20).orEmpty(),
                            stopPlace = cols.getOrNull(21).orEmpty(),
                            stopKmOdometer = cols.getOrNull(22).orEmpty()
                        )
                        if (s.stopId.isNotBlank() && s.travelId.isNotBlank()) {
                            stops.add(s)
                        }
                    }
                }
            }
        }

        return UnifiedCsvData(travels = travels, stops = stops)
    }

    /**
     * Parser CSV compatible con campos entrecomillados y comillas escapadas.
     */
    private fun parseCsvLine(line: String): List<String> {
        val out = ArrayList<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes) {
                        // Verificar si es una comilla escapada ("")
                        val nextIsQuote = (i + 1 < line.length && line[i + 1] == '"')
                        if (nextIsQuote) {
                            sb.append('"')
                            i++ // Saltar la segunda comilla
                        } else {
                            inQuotes = false
                        }
                    } else {
                        inQuotes = true
                    }
                }

                c == ',' && !inQuotes -> {
                    out.add(sb.toString())
                    sb.setLength(0)
                }

                else -> {
                    sb.append(c)
                }
            }
            i++
        }

        out.add(sb.toString())
        return out
    }
}
