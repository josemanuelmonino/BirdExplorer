package com.example.birdexplorer

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.example.birdexplorer.ui.MapViewModel
import com.example.birdexplorer.ui.MarkerType
import org.osmdroid.util.GeoPoint
import kotlin.math.ln

// Obtains actual GPS location if there is no error
private fun fetchLocation(context: Context, onLocation: (Location) -> Unit, onFail: () -> Unit) {
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

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
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

// ---------- Explore near me ----------
@Composable
fun ExploreNearMeScreen(
    viewModel: BirdViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    ScreenEffects(uiState, viewModel)

    var locating by rememberSaveable { mutableStateOf(false) }

    fun requestLocationAndLoad() {
        locating = true
        fetchLocation(
            context,
            onLocation = { loc ->
                locating = false
                viewModel.loadNearMe(loc.latitude, loc.longitude, 10)
            },
            onFail = {
                locating = false
                Toast.makeText(context, "Could not get your location", Toast.LENGTH_LONG).show()
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) requestLocationAndLoad()
        else Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
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

    // Automated loading that launches once
    LaunchedEffect(Unit) {
        if (viewModel.uiState.value.sightings.isEmpty()) checkPermissionAndLoad()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 20.dp)) {
            ScreenTitle("Birds near you")
            SightingsList(
                uiState = uiState,
                emptyText = "No sightings to show",
                onSightingClick = viewModel::selectSighting,
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        }

        // Asks for the location again
        FloatingActionButton(
            onClick = { if (!locating) checkPermissionAndLoad() },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            if (locating) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            else Icon(Icons.Default.Refresh, contentDescription = "Refresh location")
        }
    }
}

// ---------- Explore by area ----------

// BirdSighting (iNaturalist) -> marker for the map
private fun BirdSighting.toMarkerItem() = MapMarkerItem(
    id = "api_$id",
    title = commonName,
    subtitle = place ?: observedOn ?: scientificName,
    latitude = lat,
    longitude = lon,
    type = MarkerType.API_OBSERVATION
)

// Zoom that fits the whole region on screen (bigger region -> lower zoom)
private fun zoomFor(region: SpainRegion): Double {
    val span = maxOf(region.neLat - region.swLat, region.neLng - region.swLng)
    return (ln(1400.0 / span) / ln(2.0)).toInt().coerceIn(5, 10).toDouble()
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

// Bottom panel of the map: one card per sighting, like the list of Google Maps
@Composable
private fun SightingsCarousel(
    sightings: List<BirdSighting>,
    onSightingClick: (BirdSighting) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 3.dp
    ) {
        Column(Modifier.padding(vertical = 10.dp)) {
            val word = if (sightings.size == 1) "sighting" else "sightings"
            Text(
                text = "${sightings.size} $word",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sightings) { s ->
                    Card(modifier = Modifier.width(260.dp)) {
                        Row(
                            modifier = Modifier
                                .clickable { onSightingClick(s) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = s.photoUrl,
                                contentDescription = s.commonName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    s.commonName,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    s.place ?: "Unknown place",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    s.observedOn ?: "",
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
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    ScreenEffects(uiState, viewModel)

    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var showMap by rememberSaveable { mutableStateOf(false) } //list is the one active by default

    //The map always draws the same sightings as the list
    LaunchedEffect(uiState.sightings) {
        mapViewModel.setApiObservations(uiState.sightings.map { it.toMarkerItem()})
    }

    Column(modifier = modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
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
            items(spainRegions) { region ->
                FilterChip(
                    selected = selected == region.name,
                    onClick = {
                        selected = region.name
                        viewModel.loadByArea(region.neLat, region.neLng, region.swLat, region.swLng)
                        mapViewModel.onAreaCenterSelected(
                            GeoPoint(
                                (region.neLat + region.swLat) / 2,
                                (region.neLng + region.swLng) / 2
                            ),
                            zoomFor(region)
                        )
                    },
                    label = { Text(region.name) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        if (showMap) {
            Box(Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
            ) {
                ExploreAreaScreen(
                    mapViewModel = mapViewModel,
                    showSearchBar = false,
                    onMarkerClick =  { item ->
                        uiState.sightings
                            .firstOrNull { "api_${it.id}" == item.id }
                            ?.let(viewModel::selectSighting)
                    }
                )
                if (uiState.sightings.isNotEmpty()) {
                    SightingsCarousel(
                        sightings = uiState.sightings,
                        onSightingClick = viewModel::selectSighting,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
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
