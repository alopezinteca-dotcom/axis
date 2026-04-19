package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.export

import android.content.Context
import android.net.Uri

object ExportPreferences {

    private const val PREFS = "axis_export_prefs"

    // Carpeta AXIS en Drive (treeUri)
    private const val KEY_FOLDER_URI = "export_folder_uri"

    // (Compat) si alguna vez guardaste el URI del archivo diario en vez de la carpeta
    private const val KEY_DAILY_FILE_URI = "export_daily_file_uri"

    // (Compat) cierre mensual (ej: "2026_04")
    private const val KEY_LAST_CLOSED_KEY = "export_last_closed_key"

    // (Compat) backup por fecha (yyyy_MM_dd)
    private const val KEY_LAST_BACKUP_DATE = "export_last_backup_date"

    // Recomendado: backup por timestamp (millis) para calcular 7 días de forma robusta
    private const val KEY_LAST_BACKUP_TIMESTAMP = "last_backup_timestamp"

    // ---------------- Folder Uri ----------------

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

    // ---------------- (Compat) Daily File Uri ----------------

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

    // ---------------- (Compat) Monthly Close Key ----------------

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

    // ---------------- (Compat) Backup Date yyyy_MM_dd ----------------

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

    // ---------------- Backup Timestamp (millis) ----------------

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
