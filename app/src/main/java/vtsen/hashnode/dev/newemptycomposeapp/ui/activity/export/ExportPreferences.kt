package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.export

import android.content.Context
import android.net.Uri

/**
 * ExportPreferences
 * * Gestiona la persistencia de las rutas (URIs) de exportación y metadatos de backup.
 * Prioriza el uso de MASTER_FILE_URI para garantizar compatibilidad con Drive en Samsung.
 */
object ExportPreferences {

    private const val PREFS = "axis_export_prefs"

    // ✅ ESTRATEGIA PRINCIPAL: URI del ARCHIVO MAESTRO (CSV individual vinculado a Excel)
    private const val KEY_MASTER_FILE_URI = "export_master_file_uri"

    // =========================
    // Claves de Compatibilidad
    // =========================
    // Carpeta en Drive (treeUri)
    private const val KEY_FOLDER_URI = "export_folder_uri"
    // URI de archivo diario (si aplica)
    private const val KEY_DAILY_FILE_URI = "export_daily_file_uri"
    // Cierre mensual (Key de control)
    private const val KEY_LAST_CLOSED_KEY = "export_last_closed_key"
    // Fecha del último backup exitoso (yyyy_MM_dd)
    private const val KEY_LAST_BACKUP_DATE = "export_last_backup_date"
    // Timestamp del último backup (millis)
    private const val KEY_LAST_BACKUP_TIMESTAMP = "last_backup_timestamp"

    // ----------------------------------------------------------------------
    // ✅ GESTIÓN DEL ARCHIVO MAESTRO (Estrategia por Archivo Individual)
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
    // GESTIÓN DE CARPETA (Estrategia por Carpeta - Compatibilidad)
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
    // GESTIÓN DE ARCHIVO DIARIO
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
    // METADATOS DE CONTROL
    // ----------------------------------------------------------------------

    /**
     * Guarda la clave del último periodo cerrado (ej. "2026-04").
     */
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

    /**
     * Guarda la fecha del último backup (formato legible yyyy_MM_dd).
     */
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

    /**
     * Guarda el timestamp exacto del último backup.
     */
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
