package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.export

import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelEntity

object TravelCsvExporter {

    private val dateFormat =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    private val headers = listOf(
        "ID",
        "ORIGEN",
        "DESTINO",
        "DESCRIPCION",
        "KM_INICIO",
        "KM_FIN",
        "HORAS_IMPUTADAS",
        "FACTURACION",
        "DIETA",
        "ESTADO",
        "FECHA_INICIO",
        "FECHA_FIN"
    )

    fun writeCsv(
        outputStream: OutputStream,
        travels: List<TravelEntity>
    ) {
        outputStream.bufferedWriter().use { writer ->

            writer.appendLine(headers.joinToString(";"))

            travels.forEach { t ->
                val row = listOf(
                    t.id,
                    t.origin,
                    t.destination,
                    t.description.replace(";", ","),
                    t.kmStart,
                    t.kmEnd ?: "",
                    t.hoursImputed ?: "",
                    t.billingExpected,
                    if (t.hasDiet) "SI" else "NO",
                    t.status.name,
                    dateFormat.format(Date(t.startTimestamp)),
                    t.endTimestamp?.let { dateFormat.format(Date(it)) } ?: ""
                )

                writer.appendLine(row.joinToString(";"))
            }
        }
    }
}