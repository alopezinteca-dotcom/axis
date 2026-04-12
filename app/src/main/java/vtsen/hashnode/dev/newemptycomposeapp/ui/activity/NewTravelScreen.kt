package vtsen.hashnode.dev.newemptycomposeapp.ui.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTravelScreen(
    viewModel: ActivityViewModel,
    onStartTravel: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val geocoder = remember { Geocoder(context, Locale.getDefault()) }

    // Estados con rememberSaveable para rotación
    var origin by rememberSaveable { mutableStateOf("") }
    var destination by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var kmStart by rememberSaveable { mutableStateOf("") }
    var billing by rememberSaveable { mutableStateOf("") }
    var hasDiet by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isFetchingLocation by rememberSaveable { mutableStateOf(false) }

    // Validaciones derivadas (Mejora UX-A)
    val isKmError by remember { derivedStateOf { kmStart.isNotEmpty() && kmStart.toIntOrNull() == null } }
    val isBillingError by remember { 
        derivedStateOf { 
            billing.isNotEmpty() && billing.replace(',', '.').toDoubleOrNull() == null 
        } 
    }
    val canConfirm by remember {
        derivedStateOf {
            origin.isNotBlank() && 
            destination.isNotBlank() && 
            kmStart.isNotBlank() && !isKmError &&
            billing.isNotBlank() && !isBillingError &&
            !isFetchingLocation
        }
    }

    // Lógica de GPS y Geocoding
    fun fetchAddress(location: Location) {
        coroutineScope.launch {
            try {
                if (!Geocoder.isPresent()) {
                    origin = "${location.latitude}, ${location.longitude}"
                    isFetchingLocation = false
                    return@launch
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                        val address = addresses.firstOrNull()?.getAddressLine(0)
                        coroutineScope.launch(Dispatchers.Main) {
                            origin = address ?: "${location.latitude}, ${location.longitude}"
                            isFetchingLocation = false
                        }
                    }
                } else {
                    withContext(Dispatchers.IO) {
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        val address = addresses?.firstOrNull()?.getAddressLine(0)
                        withContext(Dispatchers.Main) {
                            origin = address ?: "${location.latitude}, ${location.longitude}"
                            isFetchingLocation = false
                        }
                    }
                }
            } catch (e: Exception) {
                origin = "${location.latitude}, ${location.longitude}"
                isFetchingLocation = false
            }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getFreshLocation() {
        try {
            val location = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
            if (location != null) fetchAddress(location)
            else {
                errorMessage = "GPS frío o sin señal. Inténtalo de nuevo."
                isFetchingLocation = false
            }
        } catch (e: Exception) {
            errorMessage = "Error al conectar con servicios de ubicación."
            isFetchingLocation = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.any { it }) {
            errorMessage = null
            isFetchingLocation = true
            coroutineScope.launch { getFreshLocation() }
        } else {
            errorMessage = "Permisos denegados."
        }
    }

    fun onLocationClick() {
        errorMessage = null
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fine) {
            isFetchingLocation = true
            coroutineScope.launch { getFreshLocation() }
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Módulo Activity: Nueva Salida") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // COLUMNA IZQUIERDA: RUTA Y CONTEXTO
            Card(
                modifier = Modifier.weight(1f),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Ruta y Contexto", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    OutlinedTextField(
                        value = origin,
                        onValueChange = { origin = it },
                        label = { Text("Origen") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (isFetchingLocation) CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            else IconButton(onClick = { onLocationClick() }, enabled = !isFetchingLocation) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    )

                    OutlinedTextField(
                        value = destination,
                        onValueChange = { destination = it },
                        label = { Text("Destino previsto") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descripción / Expediente") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4
                    )
                }
            }

            // COLUMNA DERECHA: ECONOMÍA Y ACCIÓN
            Card(
                modifier = Modifier.weight(1f),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Métricas y Economía", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = kmStart,
                        onValueChange = { kmStart = it },
                        label = { Text("Kilómetros inicio") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = isKmError,
                        supportingText = { if (isKmError) Text("Introduce un número entero") }
                    )

                    OutlinedTextField(
                        value = billing,
                        onValueChange = { billing = it },
                        label = { Text("Facturación (€)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = isBillingError,
                        supportingText = { if (isBillingError) Text("Usa punto o coma para decimales") }
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = hasDiet, onCheckedChange = { hasDiet = it })
                        Text("Incluir dieta fija")
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    if (errorMessage != null) {
                        Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    Button(
                        onClick = {
                            // Mejora UX-B: Limpiar la coma antes de guardar
                            val cleanBilling = billing.replace(',', '.').toDoubleOrNull() ?: 0.0
                            val success = viewModel.startTravel(
                                origin = origin,
                                destination = destination,
                                description = description,
                                kmStart = kmStart.toIntOrNull() ?: 0,
                                billingExpected = cleanBilling,
                                hasDiet = hasDiet
                            )
                            if (success) onStartTravel()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = canConfirm,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("CONFIRMAR Y EMPEZAR VIAJE", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
