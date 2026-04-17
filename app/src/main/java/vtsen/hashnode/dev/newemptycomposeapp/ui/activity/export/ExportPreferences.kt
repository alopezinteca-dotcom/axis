package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.export

import android.content.Context
import android.net.Uri

object ExportPreferences {
    private const val PREFS = "axis_export_prefs"

    // Carpeta AXIS en Drive (treeUri)
    private const val KEY_FOLDER_URI = "export_folder_uri"

    // Diario fijo (no necesario si usamos carpeta; lo dejo por compat)
    private const val KEY_DAILY_FILE_URI = "export_daily_file_uri"

    // Cierre mensual (según Desde/Hasta)
    private const val KEY_LAST_CLOSED_KEY = "export_last_closed_key"

    // Backup diario últimos 7
    private const val KEY_LAST_BACKUP_DATE = "export_last_backup_date" // yyyy_MM_dd

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

    // Compat (si alguna vez vuelves a modo "archivo fijo")
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
}
