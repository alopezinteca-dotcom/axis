package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.export

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/* =========================================================
   UTILIDADES DE EXPORTACIÓN
   - Nombre de archivo mensual fijo
   - Contrato estable con Excel/VBA
   ========================================================= */

object ExportUtils {

    /**
     * Formato:
     *  - año_mes
     *  - compatible con orden natural
     *  - fácil de consumir desde Excel/VBA
     *
     * Ejemplo:
     *  axis_2026_04.csv
     */
    private val monthFormatter =
        DateTimeFormatter.ofPattern("yyyy_MM")

    /**
     * Devuelve el nombre del CSV del mes actual.
     * Siempre el mismo durante ese mes → sobrescritura controlada.
     */
    fun currentMonthFileName(): String {
        val today = LocalDate.now()
        return "axis_${today.format(monthFormatter)}.csv"
    }
}