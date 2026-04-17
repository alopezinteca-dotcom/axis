package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.backup

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object BackupManager {

    fun backupFileName(yyyyMmDd: String): String = "AXIS_backup_$yyyyMmDd.json.gz"

    fun ensureBackupsDir(context: Context, axisTreeUri: Uri): Uri {
        // Crea o devuelve documento directorio "backups"
        val resolver = context.contentResolver
        val axisDocId = DocumentsContract.getTreeDocumentId(axisTreeUri)

        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(axisTreeUri, axisDocId)

        resolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            ),
            null, null, null
        )?.use { c ->
            while (c.moveToNext()) {
                val docId = c.getString(0)
                val name = c.getString(1)
                val mime = c.getString(2)
                if (name == "backups" && mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    return DocumentsContract.buildDocumentUriUsingTree(axisTreeUri, docId)
                }
            }
        }

        val backupsDocUri = DocumentsContract.createDocument(
            resolver,
            axisTreeUri,
            DocumentsContract.Document.MIME_TYPE_DIR,
            "backups"
        ) ?: throw IllegalStateException("No se pudo crear la carpeta backups en Drive")

        return backupsDocUri
    }

    fun writeSnapshotToGzipBytes(snapshotJson: String): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { gz ->
            gz.write(snapshotJson.toByteArray(Charsets.UTF_8))
        }
        return bos.toByteArray()
    }

    fun readGzipBytesToString(bytes: ByteArray): String {
        GZIPInputStream(bytes.inputStream()).use { gz ->
            return gz.readBytes().toString(Charsets.UTF_8)
        }
    }

    fun snapshotToJson(snapshot: BackupSnapshot): String {
        val root = JSONObject()
        root.put("createdAtMillis", snapshot.createdAtMillis)

        snapshot.billingPeriod?.let {
            val p = JSONObject()
            p.put("fromMillis", it.fromMillis)
            p.put("toMillis", it.toMillis)
            root.put("billingPeriod", p)
        } ?: run { root.put("billingPeriod", JSONObject.NULL) }

        val holArr = JSONArray()
        snapshot.holidays.forEach { h ->
            val o = JSONObject()
            o.put("date", h.date.toString())
            o.put("description", h.description)
            holArr.put(o)
        }
        root.put("holidays", holArr)

        val vacArr = JSONArray()
        snapshot.vacations.forEach { v ->
            val o = JSONObject()
            o.put("from", v.from.toString())
            o.put("to", v.to.toString())
            o.put("description", v.description)
            vacArr.put(o)
        }
        root.put("vacations", vacArr)

        val travelsArr = JSONArray()
        snapshot.travels.forEach { t ->
            // Serialización simple: guardamos los campos más importantes + el resto via toString no sirve.
            // Aquí guardamos TODOS los campos conocidos que ya están en TravelEntity (ajusta si añadiste nuevos).
            val o = JSONObject()
            o.put("id", t.id)
            o.put("startTimestamp", t.startTimestamp)
            o.put("endTimestamp", t.endTimestamp ?: JSONObject.NULL)
            o.put("origin", t.origin)
            o.put("destination", t.destination)
            o.put("description", t.description)
            o.put("kmStart", t.kmStart)
            o.put("kmEnd", t.kmEnd ?: JSONObject.NULL)
            o.put("hasDiet", t.hasDiet)
            o.put("billingExpected", t.billingExpected)
            o.put("status", t.status.name)
            o.put("hoursDraft", t.hoursDraft ?: JSONObject.NULL)
            o.put("hoursCalculatedSnapshot", t.hoursCalculatedSnapshot ?: JSONObject.NULL)
            o.put("hoursImputed", t.hoursImputed ?: JSONObject.NULL)
            o.put("hoursModified", t.hoursModified)
            o.put("deltaHours", t.deltaHours ?: JSONObject.NULL)
            o.put("impactEuroAlejandro", t.impactEuroAlejandro ?: JSONObject.NULL)
            o.put("snapCosteKmOperativo", t.snapCosteKmOperativo ?: JSONObject.NULL)
            o.put("snapCosteDietaFija", t.snapCosteDietaFija ?: JSONObject.NULL)
            o.put("snapPorcBenefExigidoA", t.snapPorcBenefExigidoA ?: JSONObject.NULL)
            o.put("snapCosteHoraAlejandro", t.snapCosteHoraAlejandro ?: JSONObject.NULL)
            o.put("snapCosteHoraEmpresaX", t.snapCosteHoraEmpresaX ?: JSONObject.NULL)
            o.put("snapTarifaObjetivoY", t.snapTarifaObjetivoY ?: JSONObject.NULL)
            o.put("isInvoiced", t.isInvoiced)
            o.put("endAddress", t.endAddress)
            travelsArr.put(o)
        }
        root.put("travels", travelsArr)

        val stopsArr = JSONArray()
        snapshot.stops.forEach { s ->
            val o = JSONObject()
            o.put("id", s.id)
            o.put("travelId", s.travelId)
            o.put("timestamp", s.timestamp)
            o.put("place", s.place)
            o.put("kmOdometer", s.kmOdometer ?: JSONObject.NULL)
            stopsArr.put(o)
        }
        root.put("stops", stopsArr)

        return root.toString()
    }

    fun writeBytesToDocument(context: Context, parentDirUri: Uri, displayName: String, mimeType: String, bytes: ByteArray) {
        val resolver = context.contentResolver

        // borrar si existe
        val parentDocId = DocumentsContract.getDocumentId(parentDirUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(parentDirUri, parentDocId)

        resolver.query(
            childrenUri,
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null, null, null
        )?.use { c ->
            while (c.moveToNext()) {
                val docId = c.getString(0)
                val name = c.getString(1)
                if (name == displayName) {
                    val docUri = DocumentsContract.buildDocumentUriUsingTree(parentDirUri, docId)
                    DocumentsContract.deleteDocument(resolver, docUri)
                    break
                }
            }
        }

        val newFileUri = DocumentsContract.createDocument(resolver, parentDirUri, mimeType, displayName)
            ?: throw IllegalStateException("No se pudo crear $displayName")

        resolver.openOutputStream(newFileUri)?.use { out ->
            BufferedOutputStream(out).use { bout ->
                bout.write(bytes)
                bout.flush()
            }
        } ?: throw IllegalStateException("No se pudo abrir OutputStream para $displayName")
    }

    fun readBytesFromUri(context: Context, uri: Uri): ByteArray {
        val resolver = context.contentResolver
        resolver.openInputStream(uri)?.use { ins ->
            BufferedInputStream(ins).use { bin ->
                return bin.readBytes()
            }
        } ?: throw IllegalStateException("No se pudo abrir InputStream del backup")
    }
}
