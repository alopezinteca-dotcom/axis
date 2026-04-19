package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.export

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.io.OutputStream
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelEntity
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelStopEntity

/**
 * AxisFileManager
 *
 * Responsabilidad única: operar con el ContentResolver + SAF (incluye Drive si soporta TREE).
 * - Buscar un archivo por nombre dentro de una carpeta (treeUri)
 * - Crear archivo si no existe
 * - SOBRESCRIBIR si existe (sin borrar) para mantener ID estable en Drive
 */
object AxisFileManager {

    private const val MIME_CSV = "text/csv"

    /**
     * Exporta CSV unificado (travels + stops) a una carpeta SAF (treeUri).
     * Drive-safe:
     * - Si existe el archivo -> sobrescribe (mantiene ID)
     * - Si no existe -> crea
     */
    fun writeUnifiedCsvOverwritingIfExists(
        context: Context,
        folderUri: Uri,
        fileName: String,
        travels: List<TravelEntity>,
        stops: List<TravelStopEntity>
    ) {
        val targetUri = findChildDocumentUriByName(context, folderUri, fileName)
            ?: createDocumentOrThrow(context, folderUri, fileName, MIME_CSV)

        openOutputStreamOrThrow(context, targetUri, truncate = true).use { os ->
            AxisUnifiedCsvExporter.writeCsv(os, travels, stops)
        }
    }

    /**
     * Busca dentro de folderUri (treeUri) un archivo con displayName == fileName.
     * Devuelve la Uri del documento o null si no existe.
     */
    fun findChildDocumentUriByName(
        context: Context,
        folderUri: Uri,
        fileName: String
    ): Uri? {
        val resolver = context.contentResolver

        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            folderUri,
            DocumentsContract.getTreeDocumentId(folderUri)
        )

        resolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME
            ),
            null,
            null,
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val documentId = cursor.getString(0)
                val displayName = cursor.getString(1)
                if (displayName == fileName) {
                    return DocumentsContract.buildDocumentUriUsingTree(folderUri, documentId)
                }
            }
        }

        return null
    }

    /**
     * Crea un documento dentro de la carpeta SAF.
     */
    fun createDocumentOrThrow(
        context: Context,
        folderUri: Uri,
        fileName: String,
        mimeType: String = MIME_CSV
    ): Uri {
        val resolver = context.contentResolver
        return DocumentsContract.createDocument(resolver, folderUri, mimeType, fileName)
            ?: throw IllegalStateException("No se pudo crear el archivo '$fileName' en la carpeta seleccionada.")
    }

    /**
     * Abre output stream con modo truncate si es posible.
     * - truncate = true -> "wt" (write + truncate), sobrescribe el contenido
     * - truncate = false -> modo por defecto
     */
    fun openOutputStreamOrThrow(
        context: Context,
        fileUri: Uri,
        truncate: Boolean
    ): OutputStream {
        val resolver = context.contentResolver
        val stream = if (truncate) {
            // "wt" = write + truncate (sobrescribe). Mantiene el mismo documento (ID estable).
            resolver.openOutputStream(fileUri, "wt")
        } else {
            resolver.openOutputStream(fileUri)
        }
        return stream ?: throw IllegalStateException("No se pudo abrir OutputStream para escribir en '$fileUri'")
    }
}
