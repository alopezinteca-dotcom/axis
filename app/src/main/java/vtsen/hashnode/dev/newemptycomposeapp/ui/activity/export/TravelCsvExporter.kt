package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.export

import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelEntity

object TravelCsvExporter {

    // Formato ISO estándar (garantiza que Excel y VBA lo entiendan siempre igual)
    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    // 🟢 FUNCIÓN PRO: "CSV Hardening"
    // Limpia cualquier texto que venga del usuario para que sea imposible romper el CSV
    private fun String.sanitizeCsv(): String {
        return this.replace(";", ",")       // Evita que se creen nuevas columnas
                   .replace("\n", " ")      // Mata los saltos de línea
                   .replace("\r", " ")      // Mata los retornos de carro
                   .replace("\"", "\"\"")   // Escapa comillas dobles
    }

    fun writeCsv(
        outputStream: OutputStream,
        travels: List<TravelEntity>
    ) {
        // 🟢 FORZAR UTF-8: Protege las tildes y las eñes (Málaga, Logroño...)
        outputStream.writer(Charsets.UTF_8).buffered().use { writer ->

            // Cabecera EXACTA e inamovible
            writer.appendLine(
                "ID;ORIGEN;DESTINO;DESCRIPCION;KM_INICIO;KM_FIN;" +
                    "HORAS_IMPUTADAS;FACTURACION;DIETA;ESTADO;FECHA_INICIO;FECHA_FIN"
            )

            travels.forEach { t ->
                // Formato de 2 decimales y forzando el punto (.)
                val hoursStr = t.hoursImputed?.let { String.format(Locale.US, "%.2f", it) } ?: ""
                val billingStr = String.format(Locale.US, "%.2f", t.billingExpected)

                // Aplicamos el sanitizeCsv() a todos los campos de texto libre
                writer.appendLine(
                    listOf(
                        t.id,
                        t.origin.sanitizeCsv(),
                        t.destination.sanitizeCsv(),
                        t.description.sanitizeCsv(),
                        t.kmStart,
                        t.kmEnd ?: "",
                        hoursStr,
                        billingStr,
                        if (t.hasDiet) "SI" else "NO",
                        t.status.name.sanitizeCsv(),
                        formatter.format(Date(t.startTimestamp)),
                        t.endTimestamp?.let { formatter.format(Date(it)) } ?: ""
                    ).joinToString(";")
                )
            }
        }
    }
}
