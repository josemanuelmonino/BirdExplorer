package com.example.birdexplorer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@Composable
fun BirdTestScreen(
    viewModel: BirdViewModel,
    onBack: () -> Unit,                         // lets the navigation go back
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    val context = LocalContext.current

    // Shows a warning on the phone every time a new error appears
        LaunchedEffect(uiState.error) {
            uiState.error?.let { message ->
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }

    var query by rememberSaveable { mutableStateOf("") }

    // Initial Load: Madrid
    // only loads once, if there is no data yet, so it doesn't reload when rotating the screen
    LaunchedEffect(Unit) {
        if (viewModel.uiState.value.sightings.isEmpty()) {
            viewModel.loadNearMe(40.4168, -3.7038, 20)
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(8.dp)) {

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onBack) { Text("Back") }       // back button
            Button(onClick = { viewModel.loadNearMe(40.4168, -3.7038, 20) }) { Text("Madrid") }
            Button(onClick = { viewModel.loadNearMe(41.3874, 2.1686, 20) }) { Text("Barcelona") }
        }

        // composable with a box outlined
        // not a button, the search it's executed without pressing anything
        OutlinedTextField(
            value = query,      // text to show
            onValueChange = { query = it; viewModel.searchSpecies(it) },
            label = { Text("Buscar especie") },
            modifier = Modifier.fillMaxWidth()
        )

        // Suggestions on the search: one press loads the sightings
        uiState.speciesResults.take(5).forEach { taxon ->
            Text(
                text = taxon.preferred_common_name ?: taxon.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.loadBySpecies(taxon.id.toInt()) }
                    .padding(8.dp)
            )
        }

        when {
            uiState.isLoading -> CircularProgressIndicator()
            uiState.error != null -> Text(uiState.error!!)
            uiState.sightings.isEmpty() -> Text("Without Sightings")
            else -> LazyColumn {
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
}