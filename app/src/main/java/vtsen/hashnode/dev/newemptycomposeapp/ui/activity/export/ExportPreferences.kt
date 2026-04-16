package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.export

import android.content.Context
import android.net.Uri

object ExportPreferences {
    private const val PREFS = "axis_export_prefs"
    private const val KEY_FOLDER_URI = "export_folder_uri"
    private const val KEY_LAST_CLOSED_MONTH = "last_closed_month"

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

    // ✅ NUEVO: mes cerrado (yyyy_MM)
    fun setLastClosedMonth(context: Context, yyyyMM: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_CLOSED_MONTH, yyyyMM)
            .apply()
    }

    fun getLastClosedMonth(context: Context): String? {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LAST_CLOSED_MONTH, null)
    }
}
