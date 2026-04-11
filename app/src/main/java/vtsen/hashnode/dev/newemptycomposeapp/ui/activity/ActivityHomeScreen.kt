package vtsen.hashnode.dev.newemptycomposeapp.ui.activity

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelEntity
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.export.ExportPreferences
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.export.ExportUtils
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.export.TravelCsvExporter

@Composable
fun ActivityHomeScreen(
    viewModel: ActivityViewModel,
    onNewTravelClick: () -> Unit
) {
    val context = LocalContext.current
    val currentTravel by viewModel.currentTravel.collectAsStateWithLifecycle()
    val closedTravels by viewModel.exportData.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.prepareExport()
    }

    val folderPickerLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocumentTree()
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

            Text(
                text = "Resumen Activity",
                style = MaterialTheme.typography.headlineMedium
            )

            currentTravel?.let {
                Card {
                    Column(Modifier.padding(16.dp)) {
                        Text("Viaje en curso", style = MaterialTheme.typography.titleMedium)
                        Text("${it.origin} → ${it.destination}")
                        Text("KM inicio: ${it.kmStart}")
                    }
                }
            }

            Text(
                text = "Viajes cerrados este mes: ${closedTravels.size}",
                style = MaterialTheme.typography.titleMedium
            )

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
                Text("Exportar mes actual a Excel")
            }

            LazyColumn {
                items(closedTravels) { travel ->
                    ClosedTravelItem(travel)
                }
            }
        }
    }
}

@Composable
private fun ClosedTravelItem(travel: TravelEntity) {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    val dateText = travel.endTimestamp?.let {
        Instant.ofEpochMilli(it)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(formatter)
    } ?: ""

    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("${travel.origin} → ${travel.destination}")
                Text(dateText, style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${travel.hoursImputed ?: 0.0} h")
                Text("${travel.billingExpected} €")
            }
        }
    }
}

/* =========================================================
   EXPORTACIÓN CSV MENSUAL CON SOBRESCRITURA REAL
   ========================================================= */

private fun exportMonthlyCsv(
    context: Context,
    folderUri: Uri,
    travels: List<TravelEntity>
) {
    val resolver = context.contentResolver
    val fileName = ExportUtils.currentMonthFileName()

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
                val fileUri =
                    DocumentsContract.buildDocumentUriUsingTree(folderUri, documentId)
                DocumentsContract.deleteDocument(resolver, fileUri)
                break
            }
        }
    }

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
    }
}
