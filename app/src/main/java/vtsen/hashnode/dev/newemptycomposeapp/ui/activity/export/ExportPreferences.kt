package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.exportpackage vtsen.hashnode.dev.newemptycomposeapp context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_MASTER_FILE_URI)
            .apply()
    }

    // ----------------------------------------------------------------------
    // Compat: Folder URI
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
        return s?.let(Uri::parse)
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
        return s?.let(Uri::parse)
    }

    fun clearDailyFileUri(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_DAILY_FILE_URI)
            .apply()
    }

    // ----------------------------------------------------------------------
    // Compat: Monthly close key
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

    // ----------------------------------------------------------------------
    // Compat: Backup date yyyy_MM_dd
    // ----------------------------------------------------------------------
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

    // ----------------------------------------------------------------------
    // Backup timestamp (millis)
    // ----------------------------------------------------------------------
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

import android.content.Context
import android.net.Uri

object ExportPreferences {
    private const val PREFS = "axis_export_prefs"

    // =========================
    // NUEVO (recomendado): URI del ARCHIVO MAESTRO (CSV fijo)
    // =========================
    private const val KEY_MASTER_FILE_URI = "export_master_file_uri"

    // =========================
    // Compatibilidad (lo que ya tenías)
    // =========================
    // Carpeta en Drive (treeUri) — puede no funcionar en tu Tab para Drive, pero lo dejamos por compat
    private const val KEY_FOLDER_URI = "export_folder_uri"

    // (Compat) URI de archivo diario si alguna vez lo usaste
    private const val KEY_DAILY_FILE_URI = "export_daily_file_uri"

    // (Compat) cierre mensual
    private const val KEY_LAST_CLOSED_KEY = "export_last_closed_key"

    // (Compat) backup por fecha yyyy_MM_dd
    private const val KEY_LAST_BACKUP_DATE = "export_last_backup_date"

    // Backup por timestamp (millis)
    private const val KEY_LAST_BACKUP_TIMESTAMP = "last_backup_timestamp"

    // ----------------------------------------------------------------------
    // ✅ MASTER FILE URI (lo que usaremos ahora)
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
        return s?.let(Uri::parse)
    }

    fun clearMasterFileUri(context: Context) {
