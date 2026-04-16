package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.export

import android.content.Context
import android.net.Uri

object ExportPreferences {
    private const val PREFS = "axis_export_prefs"

    // Compat (si antes guardabas carpeta)
    private const val KEY_FOLDER_URI = "export_folder_uri"

    // ✅ NUEVO: URI del archivo diario fijo
    private const val KEY_DAILY_FILE_URI = "export_daily_file_uri"

    // ✅ NUEVO: periodo cerrado (clave yyyy_MM basada en Desde/Hasta)
    private const val KEY_LAST_CLOSED_KEY = "export_last_closed_key"

    // ---------- Carpeta (compat) ----------
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

    // ---------- Diario fijo ----------
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

    // ---------- Cierre mensual según Desde/Hasta ----------
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
}
