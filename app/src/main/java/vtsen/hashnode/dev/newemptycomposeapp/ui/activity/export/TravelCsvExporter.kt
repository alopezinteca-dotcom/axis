package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.export

import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelEntity

object TravelCsvExporter {

    private val formatter =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    fun writeCsv(
        outputStream: OutputStream,
        travels: List<TravelEntity>
    ) {
        outputStream.bufferedWriter().use { writer ->

            // Cabecera EXACTA para Excel/VBA
            writer.appendLine(
                "ID;ORIGEN;DESTINO;DESCRIPCION;KM_INICIO;KM_FIN;" +
                    "HORAS_IMPUTADAS;FACTURACION;DIETA;ESTADO;FECHA_INICIO;FECHA_FIN"
            )

            travels.forEach { t ->
                writer.appendLine(
                    listOf(
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
                        formatter.format(Date(t.startTimestamp)),
                        t.endTimestamp?.let { formatter.format(Date(it)) } ?: ""
                    ).joinToString(";")
                )
            }
        }
    }
}
