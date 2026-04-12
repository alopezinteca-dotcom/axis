package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.export

import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelEntity

object TravelCsvExporter {

    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    private fun String.sanitizeCsv(): String {
        return this.replace(";", ",")
            .replace("\n", " ")
            .replace("\r", " ")
            .replace("\"", "\"\"")
    }

    private fun formatDouble(value: Double?): String =
        value?.let { String.format(Locale.US, "%.2f", it) } ?: ""

    fun writeCsv(outputStream: OutputStream, travels: List<TravelEntity>) {
        outputStream.writer(Charsets.UTF_8).buffered().use { writer ->

            writer.appendLine(
                "ID;ORIGEN;DESTINO;DESCRIPCION;KM_INICIO;KM_FIN;FACTURACION;DIETA;ESTADO;" +
                    "HORAS_DRAFT;HORAS_CALC_SNAPSHOT;HORAS_IMPUTADAS;HORAS_MODIFICADAS;DELTA_HORAS;IMPACTO_EURO;" +
                    "P_COSTE_KM_OPERATIVO_SNAP;P_COSTE_DIETA_FIJA_SNAP;P_PORC_BENEF_EXIGIDO_A_SNAP;P_COSTE_HORA_ALEJANDRO_SNAP;" +
                    "P_COSTE_HORA_EMPRESA_X_SNAP;P_TARIFA_OBJETIVO_Y_SNAP;" +
                    "FECHA_INICIO;FECHA_FIN"
            )

            travels.forEach { t ->
                writer.appendLine(
                    listOf(
                        t.id,
                        t.origin.sanitizeCsv(),
                        t.destination.sanitizeCsv(),
                        t.description.sanitizeCsv(),
                        t.kmStart,
                        t.kmEnd ?: "",
                        formatDouble(t.billingExpected),
                        if (t.hasDiet) "SI" else "NO",
                        t.status.name.sanitizeCsv(),
                        formatDouble(t.hoursDraft),
                        formatDouble(t.hoursCalculatedSnapshot),
                        formatDouble(t.hoursImputed),
                        if (t.hoursModified) "SI" else "NO",
                        formatDouble(t.deltaHours),
                        formatDouble(t.impactEuroAlejandro),

                        // ✅ snapshots
                        formatDouble(t.snapCosteKmOperativo),
                        formatDouble(t.snapCosteDietaFija),
                        formatDouble(t.snapPorcBenefExigidoA),
                        formatDouble(t.snapCosteHoraAlejandro),
                        formatDouble(t.snapCosteHoraEmpresaX),
                        formatDouble(t.snapTarifaObjetivoY),

                        formatter.format(Date(t.startTimestamp)),
                        t.endTimestamp?.let { formatter.format(Date(it)) } ?: ""
                    ).joinToString(";")
                )
            }
        }
    }
}
