package com.example.birdexplorer.ui

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
fun MapScreen(onBack: () -> Unit) {
    var selectedStyle by rememberSaveable { mutableStateOf(MapStyle.STANDARD) } //standard style by default
    var menuExpanded by remember { mutableStateOf(false) }

    // Search circle center and radius (in meters)
    var searchLat by rememberSaveable { mutableDoubleStateOf(DEFAULT_CENTER.latitude) }
    var searchLon by rememberSaveable { mutableDoubleStateOf(DEFAULT_CENTER.longitude) }
    val searchCenter = remember(searchLat, searchLon) { GeoPoint(searchLat, searchLon) }
    var radiusMeters by rememberSaveable { mutableFloatStateOf(500f) }

    //TRIAL API MARKERS REPLICATING THE API AND MQTT SAMPLES
    val sampleMarkers = remember {
        listOf(
            MapMarkerItem(
                id = "api_101",
                title = "Gorrión común (Passer domesticus)",
                subtitle = "Campus Sur UPM, Madrid",
                latitude = 40.3898,   // ~65 m from center
                longitude = -3.6282,
                type = MarkerType.API_OBSERVATION
            ),
            MapMarkerItem(
                id = "mqtt_1",
                title = "Cotorra argentina",
                subtitle = "Vistas 3 en los pinos junto a la cafetería",
                latitude = 40.3905,   // ~180 m from center
                longitude = -3.6272,
                type = MarkerType.MQTT_USER_SIGHTING
            ),
            MapMarkerItem(
                id = "api_102",
                title = "Mirlo común (Turdus merula)",
                subtitle = "Parque Lineal del Manzanares",
                latitude = 40.3865,   // ~360 m from center
                longitude = -3.6310,
                type = MarkerType.API_OBSERVATION
            )
        )
    }

    // Filter only sightings inside the active radius circle
    val visibleMarkers = remember(sampleMarkers, searchCenter, radiusMeters) {
        sampleMarkers.filter { marker ->
            val point = GeoPoint(marker.latitude, marker.longitude)
            searchCenter.distanceToAsDouble(point) <= radiusMeters
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        OsmMap(
            tileSource = selectedStyle.tileSource,
            markers = visibleMarkers,
            circleCenter = searchCenter,
            circleRadiusMeters = radiusMeters.toDouble(),
            onMapLongPress = { tappedPoint ->
                searchLat = tappedPoint.latitude
                searchLon = tappedPoint.longitude
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
                Text("${selectedStyle.label} map")
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                MapStyle.entries.forEach { style ->
                    DropdownMenuItem(
                        text = { Text(style.label) },
                        onClick = {
                            selectedStyle = style
                            menuExpanded = false
                        }
                    )
                }
            }
        }

        // Bottom card to adjust search cicle radius and inspect filtered count
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
            )
        ) {
            val radiusLabel = if (radiusMeters < 1000f) { //if radius < 1 km, shows it in m
                "${radiusMeters.roundToInt()} m"
            } else { //if radius > 1 km, shows it in km
                String.format(java.util.Locale.US, "%.1f km", radiusMeters / 1000f)
            }
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = "Search radius: $radiusLabel (${visibleMarkers.size} sightings)",
                    style = MaterialTheme.typography.titleSmall
                )
                Slider(
                    value = radiusMeters,
                    onValueChange = { radiusMeters = it },
                    valueRange = 100f..3000f
                )
            }
        }
    }
}