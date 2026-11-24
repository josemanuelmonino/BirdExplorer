package com.example.birdexplorer.ui

import android.location.Geocoder
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import java.util.Locale

private const val AREA_SEARCHED_ZOOM = 15.0 //when in Search by Area mode, results are shown with a
//higher zoom

// Geographic bounding box covering Spain (including Canary and Balearic Islands) to avoid map search
// leading to other countries (only info regarding bird sightings in Spain is available)
private const val SPAIN_MIN_LAT = 27.6
private const val SPAIN_MIN_LON = -18.2
private const val SPAIN_MAX_LAT = 43.8
private const val SPAIN_MAX_LON = 4.4

private fun isSpecificMatchInSpain(address: android.location.Address): Boolean {
    if (address.countryCode != null && !address.countryCode.equals("ES", ignoreCase = true)) {
        return false
    }
    val feature = address.featureName?.trim().orEmpty()
    val isOnlyCountry = feature.equals("Spain", ignoreCase = true) ||
            feature.equals("España", ignoreCase = true)
    return !isOnlyCountry && (
            address.locality != null ||
                    address.subAdminArea != null ||
                    address.adminArea != null ||
                    feature.isNotEmpty()
            )
}

@Composable
fun ExploreAreaScreen(
    onBack: (() -> Unit)? = null,
    showSearchBar: Boolean = true,
    onMarkerClick: ((MapMarkerItem) -> Unit)? = null,
    mapViewModel: MapViewModel = viewModel()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val uiState by mapViewModel.uiState.collectAsState()

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var menuExpanded by remember { mutableStateOf(false) }

    fun searchAreaByName() {
        val query = searchQuery.trim()
        if (query.isEmpty()) return
        focusManager.clearFocus()

        // append ", Spain" so ambiguous names, like "Retiro" (search limited to Spain)
        val spainQuery = if (
            query.contains("spain", ignoreCase = true) ||
            query.contains("españa", ignoreCase = true)
        ) {
            query
        } else {
            "$query, Spain"
        }

        val geocoder = Geocoder(context, Locale.getDefault())
        // Android's way of calling a place name to coordinates translator changed from API 33 (TIRAMISU; Android 13) on
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // if API version >= 33 (TIRAMISU; Android 13)
            // asynchronous and modern version of getFromLocationName function
            geocoder.getFromLocationName(
                spainQuery,
                1,
                SPAIN_MIN_LAT,
                SPAIN_MIN_LON,
                SPAIN_MAX_LAT,
                SPAIN_MAX_LON
            ) { addresses ->
                val first = addresses.firstOrNull()?.takeIf { isSpecificMatchInSpain(it) }
                if (first != null) {
                    mapViewModel.onAreaCenterSelected(
                        GeoPoint(first.latitude, first.longitude),
                        AREA_SEARCHED_ZOOM
                    )
                } else {
                    scope.launch(Dispatchers.Main) {
                        Toast.makeText(context, "Area not found in Spain: $query", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else { // if API version < 33 (TIRAMISU; Android 13)
            // old version of getFromLocationName function, which blocks the screen while on the Internet
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    runCatching {
                        geocoder.getFromLocationName(
                            spainQuery,
                            1,
                            SPAIN_MIN_LAT,
                            SPAIN_MIN_LON,
                            SPAIN_MAX_LAT,
                            SPAIN_MAX_LON
                        )?.firstOrNull()
                    }.getOrNull()
                }
                val validResult = result?.takeIf { isSpecificMatchInSpain(it) }
                if (validResult != null) {
                    mapViewModel.onAreaCenterSelected(
                        GeoPoint(validResult.latitude, validResult.longitude),
                        AREA_SEARCHED_ZOOM
                    )
                } else {
                    Toast.makeText(context, "Area not found in Spain: $query", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val allMarkers = uiState.allAreaMarkers

    Box(modifier = Modifier.fillMaxSize()) {
        OsmMap(
            center = uiState.areaCenter,
            zoom = uiState.areaZoom,
            recenterTrigger = uiState.areaRecenterTrigger,
            tileSource = uiState.selectedStyle.tileSource,
            markers = allMarkers,
            userLocation = uiState.userLocation,
            circleCenter = null, // No radius circle in Explore by Area mode
            onMarkerClick = onMarkerClick
        )

        // Top controls: Back (only if a callback is given) + Map style selector
        if (onBack != null) {
            Button(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Text("Back")
            }
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

        if (showSearchBar) {
            // Bottom card: Area search bar + regional sightings summary
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                )
        ) {
                val count = allMarkers.size
                val sightingWord = if (count == 1) "sighting" else "sightings"

                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        text = "Explore by Area ($count regional $sightingWord)",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            placeholder = { Text("City, park, or region...") },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { searchAreaByName() })
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { searchAreaByName() }) {
                            Text("Go")
                        }
                    }
                }
            }
        }
    }
}