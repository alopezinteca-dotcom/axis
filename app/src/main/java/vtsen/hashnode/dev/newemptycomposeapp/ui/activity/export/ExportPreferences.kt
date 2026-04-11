package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.export

import android.content.Context
import android.content.Intent
import android.net.Uri

object ExportPreferences {

    private const val PREFS = "axis_export_prefs"
    private const val KEY_FOLDER_URI = "export_folder_uri"

    fun saveFolderUri(context: Context, uri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FOLDER_URI, uri.toString())
            .apply()
    }

    fun getFolderUri(context: Context): Uri? {
        val uri = context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_FOLDER_URI, null)

        return uri?.let { Uri.parse(it) }
    }
}