package com.example.birdexplorer.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import kotlin.math.roundToInt

//osmdroid's USGS_SAT for satellite view belongs to the US, only had satellite view of places inside the US
//using Esri World Imagery we can use satellite view for Spain
val ESRI_SATELLITE: ITileSource = object : OnlineTileSourceBase(
    "EsriWorldImagery",
    0, 19, 256, ".jpg",
    arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/")
) {
    override fun getTileURLString(pTileIndex: Long): String {
        return baseUrl +
                MapTileIndex.getZoom(pTileIndex) + "/" +
                MapTileIndex.getY(pTileIndex) + "/" +
                MapTileIndex.getX(pTileIndex)
    }
}

enum class MapStyle(val label: String, val tileSource: ITileSource) {
    STANDARD("Standard", TileSourceFactory.MAPNIK),
    TOPO("Topographic", TileSourceFactory.OpenTopo),
    SATELLITE("Satellite", ESRI_SATELLITE) //USGS_SAT belongs to the US, only had
                                                            //satellite view of places inside the US
}

@Composable
fun MapScreen(
    onBack: () -> Unit,
    mapViewModel: MapViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by mapViewModel.uiState.collectAsState()
    var menuExpanded by remember { mutableStateOf(false) }

    // Helper function reading LocationManager (GPS first (more precise, but will fail in locations
    // with no signal), NETWORK fallback (less precise, uses nearby WiFi networks and mobile towers,
    // works in places with no GPS signal))
    fun fetchDeviceLocation() {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
            return
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        var location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (location == null) {
            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        }

        if (location != null) {
            mapViewModel.onDeviceLocationUpdated(GeoPoint(location.latitude, location.longitude ))
        } else {
            Toast.makeText(context, "Location not available", Toast.LENGTH_SHORT).show()
        }
    }

        val locationPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val isGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (isGranted) {
                fetchDeviceLocation()
            } else {
                Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    // Filter only sightings inside the active radius circle
    val visibleMarkers = uiState.visibleLocalMarkers

    Box(modifier = Modifier.fillMaxSize()) {
        OsmMap(
            center = uiState.searchCenter,
            zoom = uiState.zoom,
            recenterTrigger = uiState.recenterTrigger,
            tileSource = uiState.selectedStyle.tileSource,
            markers = visibleMarkers,
            userLocation = uiState.userLocation,
            circleCenter = uiState.searchCenter,
            circleRadiusMeters = uiState.radiusMeters.toDouble(),
            onMapLongPress = { tappedPoint ->
                mapViewModel.onSearchCenterChanged(tappedPoint)
            },
            onZoomChanged = { newZoom ->
                mapViewModel.onZoomChanged(newZoom)
            }
        )

        Button(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Text("Back")
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            FilledTonalButton(onClick = { menuExpanded = true }) {
                Text("${uiState.selectedStyle.label} map")
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                MapStyle.entries.forEach { style ->
                    DropdownMenuItem(
                        text = { Text(style.label) },
                        onClick = {
                            mapViewModel.onMapStyleSelected(style)
                            menuExpanded = false
                        }
                    )
                }
            }
        }

        // Bottom controls: "My location" button + card to adjust search cicle radius and inspect filtered count
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            Button(
                onClick = {
                    val hasFine = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    val hasCoarse = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasFine || hasCoarse) {
                        fetchDeviceLocation()
                    } else {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text("My location")
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                )
            ) {
                val radiusLabel = if (uiState.radiusMeters < 1000f) { //if radius < 1 km, shows it in m
                    "${uiState.radiusMeters.roundToInt()} m"
                } else { //if radius > 1 km, shows it in km
                    String.format(java.util.Locale.US, "%.1f km", uiState.radiusMeters / 1000f)
                }
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    val count = visibleMarkers.size
                    val sightingWord = if (count == 1) "sighting" else "sightings"
                    Text(
                        text = "Search radius: $radiusLabel ($count $sightingWord)",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Slider(
                        value = uiState.radiusMeters,
                        onValueChange = { mapViewModel.onRadiusChanged(it) },
                        valueRange = 100f..3000f
                    )
                }
            }
        }
    }
}