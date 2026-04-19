package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.export

import android.content.Context
import android.net.Uri

/**
 * ExportPreferences
 * * Gestiona la persistencia de las rutas de exportación y metadatos de backup.
 * Se ha añadido soporte para MASTER_FILE_URI como solución definitiva para
 * dispositivos Samsung que ocultan Drive en el selector de carpetas.
 */
object ExportPreferences {

    private const val PREFS = "axis_export_prefs"

    // ✅ NUEVO: URI del ARCHIVO MAESTRO (CSV fijo vinculado a Excel)
    private const val KEY_MASTER_FILE_URI = "export_master_file_uri"

    // =========================
    // Claves de Compatibilidad
    // =========================
    private const val KEY_FOLDER_URI = "export_folder_uri"
    private const val KEY_DAILY_FILE_URI = "export_daily_file_uri"
    private const val KEY_LAST_CLOSED_KEY = "export_last_closed_key"
    private const val KEY_LAST_BACKUP_DATE = "export_last_backup_date"
    private const val KEY_LAST_BACKUP_TIMESTAMP = "last_backup_timestamp"

    // ----------------------------------------------------------------------
    // ✅ MASTER FILE URI (Estrategia por Archivo Individual)
    // ----------------------------------------------------------------------

    fun saveMasterFileUri(context: Context, uri: Uri) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MASTER_FILE_URI, uri.toString())
            .apply()
    }

    fun getMasterFileUri(context: Context): Uri? {
        val s = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_MASTER_FILE_URI, null)
            
        return s?.let { Uri.parse(it) }
    }

    fun clearMasterFileUri(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_MASTER_FILE_URI)
            .apply()
    }

    // ----------------------------------------------------------------------
    // Compat: Folder URI (Estrategia por Carpeta)
    // ----------------------------------------------------------------------

    fun saveFolderUri(context: Context, uri: Uri) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FOLDER_URI, uri.toString())
            .apply()
    }

    fun getFolderUri(context: Context): Uri? {
        val s = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_FOLDER_URI, null)
            
        return s?.let { Uri.parse(it) }
    }

    fun clearFolderUri(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_FOLDER_URI)
            .apply()
    }

    // ----------------------------------------------------------------------
    // Compat: Daily file URI
    // ----------------------------------------------------------------------

    fun saveDailyFileUri(context: Context, uri: Uri) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DAILY_FILE_URI, uri.toString())
            .apply()
    }

    fun getDailyFileUri(context: Context): Uri? {
        val s = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_DAILY_FILE_URI, null)
            
        return s?.let { Uri.parse(it) }
    }

    fun clearDailyFileUri(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_DAILY_FILE_URI)
            .apply()
    }

    // ----------------------------------------------------------------------
    // Metadatos: Cierre Mensual y Backup
    // ----------------------------------------------------------------------

    fun setLastClosedKey(context: Context, key: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_CLOSED_KEY, key)
            .apply()
    }

    fun getLastClosedKey(context: Context): String? {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LAST_CLOSED_KEY, null)
    }

    fun setLastBackupDate(context: Context, yyyyMmDd: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_BACKUP_DATE, yyyyMmDd)
            .apply()
    }

    fun getLastBackupDate(context: Context): String? {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LAST_BACKUP_DATE, null)
    }

    fun setLastBackupTimestamp(context: Context, timestampMillis: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_BACKUP_TIMESTAMP, timestampMillis)
            .apply()
    }

    fun getLastBackupTimestamp(context: Context): Long {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_BACKUP_TIMESTAMP, 0L)
    }
}
