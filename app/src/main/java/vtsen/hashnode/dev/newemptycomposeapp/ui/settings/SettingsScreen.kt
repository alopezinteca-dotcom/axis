package vtsen.hashnode.dev.newemptycomposeapp.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Inputs (rememberSaveable para rotación)
    var costeKm by rememberSaveable { androidx.compose.runtime.mutableStateOf("") }
    var dietaFija by rememberSaveable { androidx.compose.runtime.mutableStateOf("") }
    var benefA by rememberSaveable { androidx.compose.runtime.mutableStateOf("") }
    var costeHoraA by rememberSaveable { androidx.compose.runtime.mutableStateOf("") }

    var salario by rememberSaveable { androidx.compose.runtime.mutableStateOf("") }
    var cargas by rememberSaveable { androidx.compose.runtime.mutableStateOf("") }
    var overhead by rememberSaveable { androidx.compose.runtime.mutableStateOf("") }
    var horasAnuales by rememberSaveable { androidx.compose.runtime.mutableStateOf("") }
    var utilizacion by rememberSaveable { androidx.compose.runtime.mutableStateOf("") }
    var margenEmp by rememberSaveable { androidx.compose.runtime.mutableStateOf("") }

    // Cargar valores actuales
    LaunchedEffect(settings) {
        costeKm = settings.costeKmOperativo.toString()
        dietaFija = settings.costeDietaFija.toString()
        benefA = settings.porcBenefExigidoA.toString()
        costeHoraA = settings.costeHoraAlejandro.toString()

        salario = settings.salarioBrutoAnual.toString()
        cargas = settings.cargasEmpresa.toString()
        overhead = settings.overheadAnual.toString()
        horasAnuales = settings.horasAnuales.toString()
        utilizacion = settings.utilizacion.toString()
        margenEmp = settings.margenEmpresa.toString()
    }

    fun toDouble(text: String): Double? = text.replace(',', '.').toDoubleOrNull()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Ajustes AXIS") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {

                // =========================
                // Card Modelo A (Alejandro)
                // =========================
                Card(
                    modifier = Modifier.weight(1f),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Modelo A (Alejandro)", fontWeight = FontWeight.Bold)

                        OutlinedTextField(
                            value = costeKm,
                            onValueChange = { costeKm = it },
                            label = { Text("P_COSTE_KM_OPERATIVO (€/km)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = dietaFija,
                            onValueChange = { dietaFija = it },
                            label = { Text("P_COSTE_DIETA_FIJA (€)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = benefA,
                            onValueChange = { benefA = it },
                            label = { Text("P_PORC_BENEF_EXIGIDO_A (ej: 0.35)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = costeHoraA,
                            onValueChange = { costeHoraA = it },
                            label = { Text("P_COSTE_HORA_ALEJANDRO (€/h)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // =========================
                // Card Modelo Empresa
                // =========================
                Card(
                    modifier = Modifier.weight(1f),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Modelo Empresa", fontWeight = FontWeight.Bold)

                        OutlinedTextField(
                            value = salario,
                            onValueChange = { salario = it },
                            label = { Text("P_SALARIO_BRUTO_ANUAL (€)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = cargas,
                            onValueChange = { cargas = it },
                            label = { Text("P_CARGAS_EMPRESA (ej: 0.3065)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = overhead,
                            onValueChange = { overhead = it },
                            label = { Text("P_OVERHEAD_ANUAL (€)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = horasAnuales,
                            onValueChange = { horasAnuales = it },
                            label = { Text("P_HORAS_ANUALES (h)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = utilizacion,
                            onValueChange = { utilizacion = it },
                            label = { Text("P_UTILIZACION (ej: 0.80)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = margenEmp,
                            onValueChange = { margenEmp = it },
                            label = { Text("P_MARGEN_EMPRESA (ej: 0.15)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val newSettings = AxisSettings(
                        // Modelo A
                        costeKmOperativo = toDouble(costeKm) ?: settings.costeKmOperativo,
                        costeDietaFija = toDouble(dietaFija) ?: settings.costeDietaFija,
                        porcBenefExigidoA = toDouble(benefA) ?: settings.porcBenefExigidoA,
                        costeHoraAlejandro = toDouble(costeHoraA) ?: settings.costeHoraAlejandro,

                        // Empresa
                        salarioBrutoAnual = toDouble(salario) ?: settings.salarioBrutoAnual,
                        cargasEmpresa = toDouble(cargas) ?: settings.cargasEmpresa,
                        overheadAnual = toDouble(overhead) ?: settings.overheadAnual,
                        horasAnuales = toDouble(horasAnuales) ?: settings.horasAnuales,
                        utilizacion = toDouble(utilizacion) ?: settings.utilizacion,
                        margenEmpresa = toDouble(margenEmp) ?: settings.margenEmpresa
                    )

                    viewModel.saveAll(newSettings)

                    // ✅ showSnackbar() es suspend -> corrutina
                    scope.launch {
                        snackbarHostState.showSnackbar("✅ Ajustes guardados")
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("GUARDAR AJUSTES", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("VOLVER", fontWeight = FontWeight.Bold)
            }
        }
    }
}
