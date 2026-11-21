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

private const val AREA_DEFAULT_ZOOM = 12.0

@Composable
fun ExploreAreaScreen(
    onBack: () -> Unit,
    mapViewModel: MapViewModel = viewModel()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val uiState by mapViewModel.uiState.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var menuExpanded by remember { mutableStateOf(false) }

    fun searchAreaByName() {
        val query = searchQuery.trim()
        if (query.isEmpty()) return
        focusManager.clearFocus()

        val geocoder = Geocoder(context, Locale.getDefault())
        // Android's way of calling a place name to coordinates translator changed from API 33 (TIRAMISU; Android 13) on
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // if API version >= 33 (TIRAMISU; Android 13)
            // asynchronous and modern version of getFromLocationName function
            geocoder.getFromLocationName(query, 1) { addresses ->
                val first = addresses.firstOrNull()
                if (first != null) {
                    mapViewModel.onAreaCenterSelected(GeoPoint(first.latitude, first.longitude))
                } else {
                    scope.launch(Dispatchers.Main) {
                        Toast.makeText(context, "Area not found: $query", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else { // if API version < 33 (TIRAMISU; Android 13)
            // old version of getFromLocationName function, which blocks the screen while on the Internet
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    runCatching { geocoder.getFromLocationName(query, 1)?.firstOrNull() }.getOrNull()
                }
                if (result != null) {
                    mapViewModel.onAreaCenterSelected(GeoPoint(result.latitude, result.longitude))
                } else {
                    Toast.makeText(context, "Area not found: $query", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val allMarkers = uiState.allAreaMarkers

    Box(modifier = Modifier.fillMaxSize()) {
        OsmMap(
            center = uiState.areaCenter,
            zoom = AREA_DEFAULT_ZOOM,
            recenterTrigger = uiState.areaRecenterTrigger,
            tileSource = uiState.selectedStyle.tileSource,
            markers = allMarkers,
            userLocation = uiState.userLocation,
            circleCenter = null // No radius circle in Explore by Area mode
        )

        // Top controls: Back + Map style selector
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