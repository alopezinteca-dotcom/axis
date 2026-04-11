package vtsen.hashnode.dev.newemptycomposeapp.ui.activity

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelEntity
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.export.*

/* =========================================================
   PANTALLA 1 · RESUMEN ACTIVITY + EXPORTACIÓN
   ========================================================= */

@Composable
fun ActivityHomeScreen(
    viewModel: ActivityViewModel,
    onNewTravelClick: () -> Unit
) {
    val context = LocalContext.current
    val currentTravel by viewModel.currentTravel.collectAsStateWithLifecycle()
    val closedTravels by viewModel.exportData.collectAsStateWithLifecycle()

    // 🔹 Cargar datos cerrados al entrar
    LaunchedEffect(Unit) {
        viewModel.prepareExport()
    }

    // 🔹 Selector de carpeta SAF (solo la primera vez)
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            ExportPreferences.saveFolderUri(context, it)
            exportMonthlyCsv(context, it, closedTravels)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onNewTravelClick) {
                Text("+")
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Resumen Activity",
                    style = MaterialTheme.typography.displaySmall
                )

                // ---- BOTÓN EXPORTAR ----
                Button(
                    onClick = {
                        val folderUri = ExportPreferences.getFolderUri(context)
                        if (folderUri == null) {
                            folderPickerLauncher.launch(null)
                        } else {
                            exportMonthlyCsv(context, folderUri, closedTravels)
                        }
                    }
                ) {
                    Text("Exportar CSV")
                }
            }

            // ---- VIAJE EN CURSO ----
            currentTravel?.let {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Viaje en curso", style = MaterialTheme.typography.titleMedium)
                        Text("${it.origin} → ${it.destination}")
                        Text("KM inicio: ${it.kmStart}")
                    }
                }
            }

            // ---- RESUMEN MENSUAL ----
            Text(
                text = "Viajes cerrados este mes: ${closedTravels.size}",
                style = MaterialTheme.typography.titleMedium
            )

            // ---- LISTA DE VIAJES CERRADOS ----
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(closedTravels) { travel ->
                    ClosedTravelItem(travel)
                }
            }
        }
    }
}

/* =========================================================
   COMPONENTE: ITEM DE VIAJE
   ========================================================= */

@Composable
private fun ClosedTravelItem(travel: TravelEntity) {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

    val date = travel.endTimestamp?.let {
        Instant.ofEpochMilli(it)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    } ?: ""

    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("${travel.origin} → ${travel.destination}", style = MaterialTheme.typography.bodyLarge)
                Text(date, style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${travel.hoursImputed ?: 0.0} h")
                Text("${travel.billingExpected} €", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/* =========================================================
   FUNCIÓN REAL DE EXPORTACIÓN (SOBRESCRITURA SEGURA)
   ========================================================= */

private fun exportMonthlyCsv(
    context: Context,
    folderUri: Uri,
    travels: List<TravelEntity>
) {
    val fileName = ExportUtils.currentMonthFileName()
    val resolver = context.contentResolver

    // 1️⃣ Buscar y borrar archivo existente
    resolver.query(
        DocumentsContract.buildChildDocumentsUriUsingTree(
            folderUri,
            DocumentsContract.getTreeDocumentId(folderUri)
        ),
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
                val fileUri = DocumentsContract.buildDocumentUriUsingTree(
                    folderUri,
                    documentId
                )
                DocumentsContract.deleteDocument(resolver, fileUri)
                break
            }
        }
    }

    // 2️⃣ Crear el CSV nuevo
    val newFileUri = DocumentsContract.createDocument(
        resolver,
        folderUri,
        "text/csv",
        fileName
    )

    newFileUri?.let {
        resolver.openOutputStream(it)?.let { stream ->
            TravelCsvExporter.writeCsv(stream, travels)
        }
        android.widget.Toast.makeText(context, "Exportado: $fileName", android.widget.Toast.LENGTH_SHORT).show()
    }
}