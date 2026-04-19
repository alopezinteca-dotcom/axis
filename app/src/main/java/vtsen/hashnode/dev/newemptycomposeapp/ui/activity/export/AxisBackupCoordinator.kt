package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.export

import android.content.Context
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelEntity
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelStopEntity

/**
 * AxisBackupCoordinator
 *
 * Modo carpeta (treeUri) — compatibilidad:
 * 1) Exporta SIEMPRE el archivo maestro fijo:
 *    AXIS_Master_Database.csv
 * 2) Cada 7 días (o si nunca se ha hecho), genera un backup fechado:
 *    AXIS_Backup_yyyy_MM_dd.csv
 *
 * Nota: usa AxisFileManager (Drive-safe) para sobrescribir sin borrar (mantiene ID si aplica).
 */
object AxisBackupCoordinator {

    private const val MASTER_FILE_NAME = "AXIS_Master_Database.csv"
    private const val DAYS_BETWEEN_BACKUPS = 7L

    private val backupDateFormat = SimpleDateFormat("yyyy_MM_dd", Locale.getDefault())

    fun exportMasterAndMaybeBackup(
        context: Context,
        folderUri: Uri,
        allTravels: List<TravelEntity>,
        allStops: List<TravelStopEntity>
    ) {
        // 1) Maestro fijo (para Excel)
        AxisFileManager.writeUnifiedCsvOverwritingIfExists(
            context = context,
            folderUri = folderUri,
            fileName = MASTER_FILE_NAME,
            travels = allTravels,
            stops = allStops
        )

        // 2) Backup cada 7 días
        val now = System.currentTimeMillis()
        val last = ExportPreferences.getLastBackupTimestamp(context)

        if (shouldCreateBackup(now, last)) {
            val stamp = backupDateFormat.format(Date(now))
            val backupName = "AXIS_Backup_$stamp.csv"

            AxisFileManager.writeUnifiedCsvOverwritingIfExists(
                context = context,
                folderUri = folderUri,
                fileName = backupName,
                travels = allTravels,
                stops = allStops
            )

            ExportPreferences.setLastBackupTimestamp(context, now)
        }
    }

    private fun shouldCreateBackup(nowMillis: Long, lastBackupMillis: Long): Boolean {
        if (lastBackupMillis == 0L) return true
        val diffDays = TimeUnit.MILLISECONDS.toDays(nowMillis - lastBackupMillis)
        return diffDays >= DAYS_BETWEEN_BACKUPS
    }
}
