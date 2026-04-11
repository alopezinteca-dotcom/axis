package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.export

import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelEntity

object TravelCsvExporter {

    private val formatter =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    fun writeCsv(outputStream: OutputStream, items: List<TravelEntity>) {
        outputStream.bufferedWriter().use { writer ->
            writer.appendLine("ORIGEN;DESTINO;HORAS;FACTURACION;FECHA")

            for (t in items) {
                val date = t.endTimestamp?.let { formatter.format(Date(it)) } ?: ""
                writer.appendLine(
                    "${t.origin};${t.destination};${t.hoursImputed};${t.billingExpected};$date"
                )
            }
        }
    }
}
