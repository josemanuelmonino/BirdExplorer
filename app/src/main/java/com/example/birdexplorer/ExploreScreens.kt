package com.example.birdexplorer

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import coil3.compose.AsyncImage
import com.example.birdexplorer.data.BirdSighting
import com.example.birdexplorer.data.remote.Taxon
import com.example.birdexplorer.ui.ExploreAreaScreen
import com.example.birdexplorer.ui.MapMarkerItem
import com.example.birdexplorer.ui.MapStyle
import com.example.birdexplorer.ui.MapViewModel
import com.example.birdexplorer.ui.MarkerType
import com.example.birdexplorer.ui.OsmMap
import org.osmdroid.util.GeoPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ln
import kotlin.math.roundToInt

// Obtains actual GPS location if there is no error
fun fetchLocation(context: Context, onLocation: (Location) -> Unit, onFail: () -> Unit) {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val fine = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    // Actives providers that we can use with the permissions granted
    val active = buildList {
        if (fine) add(LocationManager.GPS_PROVIDER)
        add(LocationManager.NETWORK_PROVIDER)
    }.filter { manager.isProviderEnabled(it) }

    // Last known location
    val last = (active + LocationManager.PASSIVE_PROVIDER)
        .mapNotNull { manager.getLastKnownLocation(it) }
        .maxByOrNull { it.time }
    if (last != null) { onLocation(last); return }

    // If we don't havev a active location we ask again
    if (active.isEmpty()) { onFail(); return }

    var done = false
    val signal = android.os.CancellationSignal()
    val handler = Handler(Looper.getMainLooper())
    val timeout = Runnable {
        if (!done) { done = true; signal.cancel(); onFail() }
    }
    handler.postDelayed(timeout, 15_000)

    active.forEach { provider ->
        LocationManagerCompat.getCurrentLocation(
            manager, provider, signal, ContextCompat.getMainExecutor(context)
        ) { location: Location? ->
            if (location != null && !done) {
                done = true
                handler.removeCallbacks(timeout)
                signal.cancel()
                onLocation(location)
            }
        }
    }
}

// Shows the messages of the ViewModel as Toasts and the detail of the selected sighting.
// Every screen calls it once
@Composable
private fun ScreenEffects(uiState: BirdUiState, viewModel: BirdViewModel) {
    val context = LocalContext.current

    // Connection and API problems
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    // Simple messages, for instance when there are no results
    LaunchedEffect(uiState.info) {
        uiState.info?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearInfo()
        }
    }

    uiState.selected?.let { SightingDetailSheet(it, onDismiss = viewModel::dismissDetail) }
}

// Screen title
@Composable
private fun ScreenTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

// Sightings list, a click on one opens its detail
@Composable
private fun SightingsList(
    uiState: BirdUiState,
    emptyText: String,
    onSightingClick: (BirdSighting) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        uiState.isLoading -> Box(modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        uiState.sightings.isEmpty() -> Text(emptyText, modifier = modifier)
        else -> LazyColumn(modifier) {
            items(uiState.sightings) { s ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSightingClick(s) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = s.photoUrl,
                        contentDescription = s.commonName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(72.dp).clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(s.commonName, style = MaterialTheme.typography.titleMedium)
                        Text(s.scientificName, style = MaterialTheme.typography.bodySmall)
                        Text(s.place ?: "Unknown place", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

// Species found by the search, a click on one loads its sightings
@Composable
private fun SpeciesList(
    species: List<Taxon>,
    onSpeciesClick: (Taxon) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier) {
        items(species) { taxon ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSpeciesClick(taxon) }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = taxon.default_photo?.url,
                    contentDescription = taxon.preferred_common_name ?: taxon.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp))
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        taxon.preferred_common_name ?: taxon.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(taxon.name, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

// Detail of one sighting. It is a sheet, so it can also be opened from a map marker later
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SightingDetailSheet(sighting: BirdSighting, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = sighting.photoUrl,
                    contentDescription = sighting.commonName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(max = 220.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(sighting.commonName, style = MaterialTheme.typography.titleLarge)
                    Text(
                        sighting.scientificName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic
                    )
                    Text(
                        "Date: ${sighting.observedOn ?: "Unknown"}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Place: ${sighting.place ?: "Unknown place"}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Coordinates: %.4f, %.4f".format(sighting.lat, sighting.lon),
                        style = MaterialTheme.typography.bodySmall
                    )
                    // The photo license asks to show who took it
                    sighting.attribution?.let {
                        Text("Photo: $it", style = MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        onClick = {
                            // Opens any maps app on the place of the sighting
                            val uri = Uri.parse(
                                "geo:${sighting.lat},${sighting.lon}?q=${sighting.lat},${sighting.lon}" +
                                        "(${Uri.encode(sighting.commonName)})"
                            )
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            } catch (e: ActivityNotFoundException) {
                                Toast.makeText(context, "No maps app found", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("How to get there")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AsyncImage(
                    model = sighting.photoUrl,
                    contentDescription = sighting.commonName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Text(sighting.commonName, style = MaterialTheme.typography.titleLarge)
                Text(
                    sighting.scientificName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic
                )
                Text("Date: ${sighting.observedOn ?: "Unknown"}")
                Text("Place: ${sighting.place ?: "Unknown place"}")
                Text("Coordinates: %.4f, %.4f".format(sighting.lat, sighting.lon))
                // The photo license asks to show who took it
                sighting.attribution?.let {
                    Text("Photo: $it", style = MaterialTheme.typography.bodySmall)
                }

                Button(
                    onClick = {
                        // Opens any maps app on the place of the sighting
                        val uri = Uri.parse(
                            "geo:${sighting.lat},${sighting.lon}?q=${sighting.lat},${sighting.lon}" +
                                    "(${Uri.encode(sighting.commonName)})"
                        )
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(context, "No maps app found", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("How to get there")
                }
            }
        }
    }
}

// ---------- Explore near me ----------
private const val NEAR_ME_MAX_RADIUS_KM = 10
private const val NEAR_ME_MIN_RADIUS_M = 500f
private const val NEAR_ME_MAX_RADIUS_M = 10_000f

private fun formatRadius(meters: Float): String =
    if (meters < 1000f) "${meters.roundToInt()} m"
    else String.format(Locale.US, "%.1f km", meters / 1000f)


@Composable
fun ExploreNearMeScreen(
    viewModel: BirdViewModel,
    mapViewModel: MapViewModel,
    modifier: Modifier = Modifier,
    mqttSightings: List<Sighting> = emptyList()
) {
    val uiState by viewModel.uiState.collectAsState()
    val mapState by mapViewModel.uiState.collectAsState()
    val context = LocalContext.current
    ScreenEffects(uiState, viewModel)

    var locating by rememberSaveable { mutableStateOf(false) }
    var mqttDetail by remember { mutableStateOf<Sighting?>(null) }
    var styleMenuExpanded by remember { mutableStateOf(false) }

    // Loads API sightings up to max slider radius (10 km) so sliding never triggers extra API calls
    fun loadSightingsAround(point: GeoPoint) {
        mapViewModel.onMarkerSelected(null)
        viewModel.loadNearMe(point.latitude, point.longitude, NEAR_ME_MAX_RADIUS_KM)
    }

    fun requestLocationAndLoad() {
        locating = true
        fetchLocation(
            context,
            onLocation = { loc ->
                locating = false
                val point = GeoPoint(loc.latitude, loc.longitude)
                mapViewModel.onDeviceLocationUpdated(point)
                loadSightingsAround(point)
            },
            onFail = {
                locating = false
                Toast.makeText(context, "Could not get your location", Toast.LENGTH_LONG).show()
                if (uiState.sightings.isEmpty()) {
                    loadSightingsAround(mapState.searchCenter)
                }
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) {
            requestLocationAndLoad()
        } else {
            Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
            if (uiState.sightings.isEmpty()) {
                loadSightingsAround(mapState.searchCenter)
            }
        }
    }

    fun checkPermissionAndLoad() {
        val granted = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ).any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

        if (granted) requestLocationAndLoad()
        else permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // Initial load when entering the screen
    LaunchedEffect(Unit) {
        if (mapState.userLocation == null) {
            checkPermissionAndLoad()
        } else if (uiState.sightings.isEmpty()) {
            loadSightingsAround(mapState.searchCenter)
        }
    }

    // Unified list (MQTT first, then API) filtered strictly by distance to searchCenter <= radiusMeters
    val nearbyItems = remember(
        uiState.sightings,
        mqttSightings,
        mapState.searchCenter,
        mapState.radiusMeters
    ) {
        val all = (mqttSightings.mapNotNull { it.toAreaItem() } + uiState.sightings.map { it.toAreaItem() })
            .distinctBy { it.id }

        all.filter { item ->
            mapState.searchCenter.distanceToAsDouble(
                GeoPoint(item.latitude, item.longitude)
            ) <= mapState.radiusMeters
        }
    }

    // Sync markers rendered on OsmMap with the filtered items
    LaunchedEffect(nearbyItems) {
        mapViewModel.setApiObservations(
            nearbyItems.filter { it.type == MarkerType.API_OBSERVATION }.map { it.toMarkerItem() }
        )
        mapViewModel.setMqttSightings(
            nearbyItems.filter { it.type == MarkerType.MQTT_USER_SIGHTING }.map { it.toMarkerItem() }
        )
    }

    // First tap: select + zoom to pin. Second tap on the same item: open detail sheet
    fun onItemTapped(item: AreaItem) {
        if (mapState.selectedMarkerId == item.id) {
            item.api?.let { viewModel.selectSighting(it) }
            item.mqtt?.let { mqttDetail = it }
        } else {
            mapViewModel.onMarkerSelected(item.id)
            mapViewModel.focusOnMarker(GeoPoint(item.latitude, item.longitude), FOCUS_ZOOM)
        }
    }

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var carouselExpanded by rememberSaveable { mutableStateOf(true) }

    Box(modifier = modifier.fillMaxSize()) {
        OsmMap(
            center = mapState.cameraCenter,
            zoom = mapState.zoom,
            recenterTrigger = mapState.recenterTrigger,
            tileSource = mapState.selectedStyle.tileSource,
            markers = mapState.visibleLocalMarkers,
            userLocation = mapState.userLocation,
            circleCenter = mapState.searchCenter,
            circleRadiusMeters = mapState.radiusMeters.toDouble(),
            onMapLongPress = { pressedPoint ->
                mapViewModel.onSearchCenterChanged(pressedPoint, animateCamera = true)
                loadSightingsAround(pressedPoint)
                Toast.makeText(
                    context,
                    String.format(
                        Locale.US,
                        "Search center moved (%.4f, %.4f)",
                        pressedPoint.latitude,
                        pressedPoint.longitude
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            },
            onZoomChanged = { mapViewModel.onZoomChanged(it) },
            onMarkerClick = { marker ->
                nearbyItems.firstOrNull { it.id == marker.id }?.let { onItemTapped(it) }
            },
            selectedMarkerId = mapState.selectedMarkerId
        )

        if (isLandscape) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box {
                        FilledTonalButton(
                            onClick = { styleMenuExpanded = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(mapState.selectedStyle.label, style = MaterialTheme.typography.labelMedium)
                        }
                        DropdownMenu(
                            expanded = styleMenuExpanded,
                            onDismissRequest = { styleMenuExpanded = false }
                        ) {
                            MapStyle.entries.forEach { style ->
                                DropdownMenuItem(
                                    text = { Text(style.label) },
                                    onClick = {
                                        mapViewModel.onMapStyleSelected(style)
                                        styleMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Text(
                        text = formatRadius(mapState.radiusMeters),
                        style = MaterialTheme.typography.labelLarge
                    )

                    Slider(
                        value = mapState.radiusMeters,
                        onValueChange = { raw ->
                            val snapped = ((raw / 500f).roundToInt() * 500f)
                                .coerceIn(NEAR_ME_MIN_RADIUS_M, NEAR_ME_MAX_RADIUS_M)
                            mapViewModel.onRadiusChanged(snapped)
                        },
                        valueRange = NEAR_ME_MIN_RADIUS_M..NEAR_ME_MAX_RADIUS_M,
                        steps = 18,
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = {
                            if (!locating) {
                                mapViewModel.onMarkerSelected(null)
                                checkPermissionAndLoad()
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        if (locating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("My location", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Top-left: Map style selector (Standard / Topo / Satellite)
                    Box {
                        FilledTonalButton(onClick = { styleMenuExpanded = true }) {
                            Text("${mapState.selectedStyle.label} map")
                        }
                        DropdownMenu(
                            expanded = styleMenuExpanded,
                            onDismissRequest = { styleMenuExpanded = false }
                        ) {
                            MapStyle.entries.forEach { style ->
                                DropdownMenuItem(
                                    text = { Text(style.label) },
                                    onClick = {
                                        mapViewModel.onMapStyleSelected(style)
                                        styleMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Top-right: Center on my location button
                    Button(
                        onClick = {
                            if (!locating) {
                                mapViewModel.onMarkerSelected(null)
                                checkPermissionAndLoad()
                            }
                        }
                    ) {
                        if (locating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("My location")
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                    tonalElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.width(120.dp)) {
                            Text(
                                text = "Radius: ${formatRadius(mapState.radiusMeters)}",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = "Hold map to move",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Slider(
                            value = mapState.radiusMeters,
                            onValueChange = { raw ->
                                val snapped = ((raw / 500f).roundToInt() * 500f)
                                    .coerceIn(NEAR_ME_MIN_RADIUS_M, NEAR_ME_MAX_RADIUS_M)
                                mapViewModel.onRadiusChanged(snapped)
                            },
                            valueRange = NEAR_ME_MIN_RADIUS_M..NEAR_ME_MAX_RADIUS_M,
                            steps = 18,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Bottom: Unified carousel with API + MQTT sightings inside the circle
        if (nearbyItems.isNotEmpty()) {
            if (isLandscape && !carouselExpanded) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(8.dp)
                        .clickable { carouselExpanded = true },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    tonalElevation = 3.dp
                ) {
                    val word = if (nearbyItems.size == 1) "sighting" else "sightings"
                    Text(
                        text = "${nearbyItems.size} $word - Tap to show list",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            } else {
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isLandscape) {
                        Surface(
                            modifier = Modifier
                                .padding(bottom = 4.dp)
                                .clickable { carouselExpanded = false },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
                        ) {
                            Text(
                                text = "Hide list",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                    SightingsCarousel(
                        areaItems = nearbyItems,
                        selectedId = mapState.selectedMarkerId,
                        onItemClick = { onItemTapped(it) }
                    )
                }
            }
        }

        if (uiState.isLoading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
    }

    // Detail sheet for MQTT sightings (API ones are handled by ScreenEffects)
    mqttDetail?.let { MqttDetailSheet(it, onDismiss = { mqttDetail = null }) }
}

// ---------- Explore by area ----------

private const val FOCUS_ZOOM = 16.0 //zoom applied when a sighting is selected

//One entry of the carousel/map (both for API and MQTT markers)
private data class AreaItem(
    val id: String,
    val type: MarkerType,
    val title: String,
    val scientificName: String? = null,
    val place: String,
    val extra: String, //for example, date of the sighting
    val photoUrl: String?,
    val latitude: Double,
    val longitude: Double,
    val api: BirdSighting? = null,
    val mqtt: Sighting? = null
)

private fun formatTimestamp(timestamp: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))


// BirdSighting (iNaturalist) -> marker for the map
private fun BirdSighting.toAreaItem() = AreaItem(
    id = "api_$id",
    type = MarkerType.API_OBSERVATION,
    title = commonName,
    scientificName = scientificName,
    place = place ?: "Unknown place",
    extra = observedOn ?: "",
    photoUrl = photoUrl,
    latitude = lat,
    longitude = lon,
    api = this
)

// MQTT sighting -> marker (only if it has coordinates)
private fun Sighting.toAreaItem(): AreaItem? {
    val la = lat ?: return null
    val lo = lon ?: return null
    return AreaItem(
        id = "mqtt_${timestamp}_$species",
        type = MarkerType.MQTT_USER_SIGHTING,
        title = species,
        place = location,
        extra = formatTimestamp(timestamp),
        photoUrl = null,
        latitude = la,
        longitude = lo,
        mqtt = this
    )
}

private fun AreaItem.toMarkerItem() = MapMarkerItem(
    id = id,
    title = title,
    subtitle = if (mqtt != null && mqtt.notes.isNotBlank()) "$place - ${mqtt.notes}" else place,
    latitude = latitude,
    longitude = longitude,
    type = type
)

// Zoom that fits the whole region on screen (bigger region -> lower zoom)
private fun zoomFor(region: SpainRegion): Double {
    val span = maxOf(region.neLat - region.swLat, region.neLng - region.swLng)
    return (ln(1400.0 / span) / ln(2.0)).toInt().coerceIn(5, 10).toDouble()
}

// Opens any maps app on the given place
private fun openInMaps(context: Context, lat: Double, lon: Double, label: String) {
    val uri = Uri.parse("geo:$lat,$lon?q=$lat,$lon(${Uri.encode(label)})")
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No maps app found", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun ViewToggleButton(
    text: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (active) Button(onClick = onClick, modifier = modifier) { Text(text) }
    else OutlinedButton(onClick = onClick, modifier = modifier) { Text(text) }
}

// Small label that tells where a sighting comes from
@Composable
private fun SourceBadge(type: MarkerType) {
    val isApi = type == MarkerType.API_OBSERVATION
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (isApi) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Text(
            text = if (isApi) "API" else "MQTT",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

// Detail of a sighting published by a user through MQTT
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MqttDetailSheet(sighting: Sighting, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SourceBadge(MarkerType.MQTT_USER_SIGHTING)
            Text(sighting.species, style = MaterialTheme.typography.titleLarge)
            Text("Date: ${formatTimestamp(sighting.timestamp)}")
            Text("Place: ${sighting.location}")
            if (sighting.notes.isNotBlank()) Text("Notes: ${sighting.notes}")

            val la = sighting.lat
            val lo = sighting.lon
            if (la != null && lo != null) {
                Text("Coordinates: %.4f, %.4f".format(la, lo))
                Button(
                    onClick = { openInMaps(context, la, lo, sighting.species) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("How to get there")
                }
            }
        }
    }
}

// Bottom panel of the map: one card per sighting, like the list of Google Maps
@Composable
private fun SightingsCarousel(
    areaItems: List<AreaItem>,
    selectedId: String?,
    onItemClick: (AreaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // When a pin is selected, the carousel scrolls to its card
    LaunchedEffect(selectedId) {
        val index = areaItems.indexOfFirst { it.id == selectedId }
        if (index >= 0) listState.animateScrollToItem(index)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 3.dp
    ) {
        Column(Modifier.padding(vertical = 10.dp)) {
            val word = if (areaItems.size == 1) "sighting" else "sightings"
            Text(
                text = "${areaItems.size} $word - Tap once to locate, twice for details",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(areaItems, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier.width(260.dp),
                        border = if (item.id == selectedId)
                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .clickable { onItemClick(item) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (item.photoUrl != null) {
                                AsyncImage(
                                    model = item.photoUrl,
                                    contentDescription = item.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(64.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            } else {
                                // MQTT sightings have no photo
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.ic_marker_mqtt),
                                        contentDescription = "User sighting",
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Column {
                                SourceBadge(item.type)
                                Text(
                                    item.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    item.place,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    item.extra,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExploreByAreaScreen(
    viewModel: BirdViewModel,
    mapViewModel: MapViewModel,
    modifier: Modifier = Modifier,
    mqttSightings: List<Sighting> = emptyList()
) {
    val uiState by viewModel.uiState.collectAsState()
    val mapState by mapViewModel.uiState.collectAsState()
    ScreenEffects(uiState, viewModel)

    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var showMap by rememberSaveable { mutableStateOf(false) } //list is the one active by default
    var mqttDetail by remember { mutableStateOf<Sighting?>(null) }

    val region = spainRegions.firstOrNull { it.name == selected }

    // Everything the map and the carousel show: MQTT first, then API.
    // MQTT ones are filtered by the region box, distinctBy avoids duplicates (retained messages)
    val areaItems = remember(uiState.sightings, mqttSightings, region) {
        val mqtt = mqttSightings
            .mapNotNull { it.toAreaItem() }
            .filter { m ->
                region == null || (
                        m.latitude in region.swLat..region.neLat &&
                                m.longitude in region.swLng..region.neLng
                        )
            }
        (mqtt + uiState.sightings.map { it.toAreaItem() }).distinctBy { it.id }
    }

    // The map always draws the same sightings as the carousel
    LaunchedEffect(areaItems) {
        mapViewModel.setApiObservations(
            areaItems.filter { it.type == MarkerType.API_OBSERVATION }.map { it.toMarkerItem() }
        )
        mapViewModel.setMqttSightings(
            areaItems.filter { it.type == MarkerType.MQTT_USER_SIGHTING }.map { it.toMarkerItem() }
        )
    }

    // First tap: select + zoom to the pin. Second tap on the same one: full detail
    fun onItemTapped(item: AreaItem) {
        if (mapState.selectedMarkerId == item.id) {
            item.api?.let { viewModel.selectSighting(it) }
            item.mqtt?.let { mqttDetail = it }
        } else {
            mapViewModel.onMarkerSelected(item.id)
            mapViewModel.onAreaCenterSelected(GeoPoint(item.latitude, item.longitude), FOCUS_ZOOM)
        }
    }

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var carouselExpanded by rememberSaveable { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                horizontal = if (isLandscape) 12.dp else 16.dp,
                vertical = if (isLandscape) 8.dp else 20.dp
            )
    ) {
        if (isLandscape) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ViewToggleButton(
                    "List",
                    active = !showMap,
                    onClick = { showMap = false }
                )
                ViewToggleButton(
                    "Map",
                    active = showMap,
                    onClick = { showMap = true }
                )
                //Region filter: loads the sightings and moves the map to that region
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(spainRegions) { r ->
                        FilterChip(
                            selected = selected == r.name,
                            onClick = {
                                selected = r.name
                                mapViewModel.onMarkerSelected(null)
                                viewModel.loadByArea(r.neLat, r.neLng, r.swLat, r.swLng)
                                mapViewModel.onAreaCenterSelected(
                                    GeoPoint(
                                        (r.neLat + r.swLat) / 2,
                                        (r.neLng + r.swLng) / 2
                                    ),
                                    zoomFor(r)
                                )
                            },
                            label = { Text(r.name) }
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        } else {
            ScreenTitle("Explore by area")

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ViewToggleButton(
                    "List",
                    active = !showMap,
                    onClick = { showMap = false },
                    modifier = Modifier.weight(1f)
                )
                ViewToggleButton(
                    "Map",
                    active = showMap,
                    onClick = { showMap = true },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))

            //Region filter: loads the sightings and moves the map to that region
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(spainRegions) { r ->
                    FilterChip(
                        selected = selected == r.name,
                        onClick = {
                            selected = r.name
                            mapViewModel.onMarkerSelected(null)
                            viewModel.loadByArea(r.neLat, r.neLng, r.swLat, r.swLng)
                            mapViewModel.onAreaCenterSelected(
                                GeoPoint(
                                    (r.neLat + r.swLat) / 2,
                                    (r.neLng + r.swLng) / 2
                                ),
                                zoomFor(r)
                            )
                        },
                        label = { Text(r.name) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
        }

        if (showMap) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                ExploreAreaScreen(
                    mapViewModel = mapViewModel,
                    showSearchBar = false,
                    onMarkerClick = { marker ->
                        areaItems
                            .firstOrNull { it.id == marker.id }
                            ?.let { onItemTapped(it) }
                    }
                )
                if (areaItems.isNotEmpty()) {
                    if (isLandscape && !carouselExpanded) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(8.dp)
                                .clickable { carouselExpanded = true },
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                            tonalElevation = 3.dp
                        ) {
                            val word = if (areaItems.size == 1) "sighting" else "sightings"
                            Text(
                                text = "${areaItems.size} $word - Tap to show list",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.align(Alignment.BottomCenter),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (isLandscape) {
                                Surface(
                                    modifier = Modifier
                                        .padding(bottom = 4.dp)
                                        .clickable { carouselExpanded = false },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
                                ) {
                                    Text(
                                        text = "Hide list",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            SightingsCarousel(
                                areaItems = areaItems,
                                selectedId = mapState.selectedMarkerId,
                                onItemClick = { onItemTapped(it) }
                            )
                        }
                    }
                }
                if (uiState.isLoading) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
            }
        } else {
            SightingsList(
                uiState = uiState,
                emptyText = if (selected == null) "Pick a region to see its bird sightings"
                else "No sightings to show",
                onSightingClick = viewModel::selectSighting,
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        }
    }

    //Detail of an MQTT sighting (the API ones are shown by ScreenEffects)
    mqttDetail?.let { MqttDetailSheet(it, onDismiss = { mqttDetail = null }) }
}

// ---------- Search for a species ----------
@Composable
fun SearchSpeciesScreen(
    viewModel: BirdViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    ScreenEffects(uiState, viewModel)

    var query by rememberSaveable { mutableStateOf("") }

    // Suggestions come from a local list, they do not use the API
    val suggestions = suggestionsFor(query)

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 20.dp)) {
        ScreenTitle("Search for a species")

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },       // it does not search while typing
                label = { Text("Species name") },
                supportingText = { Text("Only species seen in Spain") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.searchSpecies(query) }),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(onClick = { viewModel.searchSpecies(query) }) {
                Icon(Icons.Default.Search, contentDescription = "Search species")
            }
        }

        // A click on a suggestion writes the name in the search box
        suggestions.forEach { name ->
            Text(
                text = name,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { query = name }
                    .padding(horizontal = 8.dp, vertical = 10.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        // First the species found, once one is chosen its sightings are shown
        if (uiState.speciesResults.isNotEmpty() && !uiState.isLoading) {
            SpeciesList(
                species = uiState.speciesResults,
                onSpeciesClick = { viewModel.loadBySpecies(it.id.toInt()) },
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        } else {
            SightingsList(
                uiState = uiState,
                emptyText = "Search a species to see where it has been seen in Spain",
                onSightingClick = viewModel::selectSighting,
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        }
    }
}