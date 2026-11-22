package com.example.birdexplorer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import coil3.compose.AsyncImage

// Obtains actual GPS location if there is no error
@SuppressLint("MissingPermission")
private fun fetchLocation(context: Context, onLocation: (Location) -> Unit, onFail: () -> Unit) {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val fine = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val provider = when {
        fine && manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
        manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
        else -> null
    }
    if (provider == null) { onFail(); return }

    val signal: android.os.CancellationSignal? = null   // fija qué overload se usa

    LocationManagerCompat.getCurrentLocation(
        manager, provider, signal, ContextCompat.getMainExecutor(context)
    ) { location: Location? ->
        val result = location ?: manager.getLastKnownLocation(provider)
        if (result != null) onLocation(result) else onFail()
    }

}

// Shows a Toast in case of any error
@Composable
private fun ErrorToast(error: String?) {
    val context = LocalContext.current
    LaunchedEffect(error) {
        error?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    }
}

// Sightings list
@Composable
private fun SightingsList(uiState: BirdUiState, modifier: Modifier = Modifier) {
    when {
        uiState.isLoading -> Box(modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        uiState.sightings.isEmpty() -> Text("No sightings to show", modifier = modifier)
        else -> LazyColumn(modifier) {
            items(uiState.sightings) { s ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
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

// ---------- Explore near me ----------
@Composable
fun ExploreNearMeScreen(
    viewModel: BirdViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    ErrorToast(uiState.error)

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

    Column(modifier = modifier.fillMaxSize().padding(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onBack) { Text("Back") }
            Button(onClick = { checkPermissionAndLoad() }, enabled = !locating) {
                Text(if (locating) "Locating..." else "Refresh location")
            }
        }
        Spacer(Modifier.height(8.dp))
        SightingsList(uiState, Modifier.weight(1f).fillMaxWidth())
    }
}

// ---------- Explore by area ----------
@Composable
fun ExploreByAreaScreen(
    viewModel: BirdViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    ErrorToast(uiState.error)

    var selected by rememberSaveable { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxSize().padding(8.dp)) {
        Button(onClick = onBack) { Text("Back") }
        Spacer(Modifier.height(8.dp))
        Text("Select a region", style = MaterialTheme.typography.titleMedium)

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(spainRegions) { region ->
                FilterChip(
                    selected = selected == region.name,
                    onClick = {
                        selected = region.name
                        viewModel.loadByArea(region.neLat, region.neLng, region.swLat, region.swLng)
                    },
                    label = { Text(region.name) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        if (selected == null) {
            Text("Pick a region to see its bird sightings")
        } else {
            SightingsList(uiState, Modifier.weight(1f).fillMaxWidth())
        }
    }
}

// ---------- Search for a species ----------
@Composable
fun SearchSpeciesScreen(
    viewModel: BirdViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    ErrorToast(uiState.error)

    var query by rememberSaveable { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize().padding(8.dp)) {
        Button(onClick = onBack) { Text("Back") }
        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Species name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.searchSpecies(query) }),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { viewModel.searchSpecies(query) }) {
                Icon(Icons.Default.Search, contentDescription = "Search species")
            }
        }

        // results of the search
        uiState.speciesResults.take(5).forEach { taxon ->
            Text(
                text = taxon.preferred_common_name ?: taxon.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.loadBySpecies(taxon.id.toInt()) }
                    .padding(8.dp)
            )
        }

        Spacer(Modifier.height(8.dp))
        SightingsList(uiState, Modifier.weight(1f).fillMaxWidth())
    }
}