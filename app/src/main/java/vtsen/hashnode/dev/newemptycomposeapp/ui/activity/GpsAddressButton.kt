package vtsen.hashnode.dev.newemptycomposeapp.ui.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun GpsAddressButton(
    modifier: Modifier = Modifier,
    onError: (String) -> Unit = {},
    onAddress: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val geocoder = remember { Geocoder(context, Locale.getDefault()) }

    var isFetchingLocation by remember { mutableStateOf(false) }

    fun fetchAddress(location: Location) {
        coroutineScope.launch {
            try {
                if (!Geocoder.isPresent()) {
                    onAddress("${location.latitude}, ${location.longitude}")
                    isFetchingLocation = false
                    return@launch
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                        val address = addresses.firstOrNull()?.getAddressLine(0)
                        coroutineScope.launch {
                            onAddress(address ?: "${location.latitude}, ${location.longitude}")
                            isFetchingLocation = false
                        }
                    }
                } else {
                    withContext(Dispatchers.IO) {
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        val address = addresses?.firstOrNull()?.getAddressLine(0)
                        withContext(Dispatchers.Main) {
                            onAddress(address ?: "${location.latitude}, ${location.longitude}")
                            isFetchingLocation = false
                        }
                    }
                }
            } catch (_: Exception) {
                onAddress("${location.latitude}, ${location.longitude}")
                isFetchingLocation = false
            }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getFreshLocation() {
        try {
            val location = fusedLocationClient
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .await()

            if (location != null) {
                fetchAddress(location)
            } else {
                onError("GPS frío o sin señal. Inténtalo de nuevo.")
                isFetchingLocation = false
            }
        } catch (_: Exception) {
            onError("Error al conectar con servicios de ubicación.")
            isFetchingLocation = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.any { it }) {
            isFetchingLocation = true
            coroutineScope.launch { getFreshLocation() }
        } else {
            onError("Permisos denegados.")
        }
    }

    fun onLocationClick() {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (fine || coarse) {
            isFetchingLocation = true
            coroutineScope.launch { getFreshLocation() }
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    if (isFetchingLocation) {
        CircularProgressIndicator(modifier = modifier.then(Modifier.padding(4.dp)), strokeWidth = 2.dp)
    } else {
        IconButton(onClick = { onLocationClick() }, modifier = modifier) {
            Icon(Icons.Default.LocationOn, contentDescription = null)
        }
    }
}
