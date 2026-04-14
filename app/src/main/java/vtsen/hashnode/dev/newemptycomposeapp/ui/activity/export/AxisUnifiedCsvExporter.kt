package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.export

import java.io.OutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelEntity
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelStopEntity

object AxisUnifiedCsvExporter {

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

    /**
     * CSV único con filas de dos tipos:
     * - row_type = TRAVEL
     * - row_type = STOP
     */
    fun writeCsv(
        output: OutputStream,
        travels: List<TravelEntity>,
        stops: List<TravelStopEntity>
    ) {
        OutputStreamWriter(output, Charsets.UTF_8).use { w ->

            w.appendLine(
                "row_type,travel_id,travel_date,travel_time_start,travel_time_end,origin,destination,description,status," +
                    "km_start,km_end,km_total,has_diet,billing_expected,is_invoiced,hours_calc_snapshot,hours_imputed,hours_draft," +
                    "stop_id,stop_timestamp,stop_time,stop_place,stop_km_odometer"
            )

            // 1) VIAJES (ASC)
            travels.sortedBy { it.startTimestamp }.forEach { t ->
                val start = Date(t.startTimestamp)
                val end = t.endTimestamp?.let { Date(it) }

                val kmEnd = t.kmEnd
                val kmTotal = kmEnd?.let { (it - t.kmStart).coerceAtLeast(0) }

                w.appendLine(
                    listOf(
                        "TRAVEL",
                        t.id,
                        dateFmt.format(start),
                        timeFmt.format(start),
                        end?.let { timeFmt.format(it) } ?: "",
                        esc(t.origin),
                        esc(t.destination),
                        esc(t.description),
                        t.status.name,
                        t.kmStart.toString(),
                        kmEnd?.toString() ?: "",
                        kmTotal?.toString() ?: "",
                        if (t.hasDiet) "1" else "0",
                        fmt2(t.billingExpected),
                        if (t.isInvoiced) "1" else "0",
                        t.hoursCalculatedSnapshot?.let { fmt2(it) } ?: "",
                        t.hoursImputed?.let { fmt2(it) } ?: "",
                        t.hoursDraft?.let { fmt2(it) } ?: "",
                        "", "", "", "", "" // stop_* vacíos
                    ).joinToString(",")
                )
            }

            // 2) PARADAS (ASC)
            stops.sortedBy { it.timestamp }.forEach { s ->
                val d = Date(s.timestamp)
                w.appendLine(
                    listOf(
                        "STOP",
                        s.travelId,
                        dateFmt.format(d),
                        "", // travel_time_start (no aplica)
                        "", // travel_time_end (no aplica)
                        "", "", "", "", // viaje vacío
                        "", "", "", "", "", "", "", "", // viaje vacío
                        s.id,
                        s.timestamp.toString(),
                        timeFmt.format(d),
                        esc(s.place),
                        s.kmOdometer?.toString() ?: ""
                    ).joinToString(",")
                )
            }

            w.flush()
        }
    }

    private fun fmt2(v: Double): String = String.format(Locale.US, "%.2f", v)

    private fun esc(v: String): String {
        val x = v.replace("\"", "\"\"")
        return "\"$x\""
    }
}
