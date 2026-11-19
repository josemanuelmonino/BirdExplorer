package com.example.birdexplorer

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

@Composable
fun HomeScreen(
    onExploreNearMe: () -> Unit,
    onExploreByArea: () -> Unit,
    onSearchSpecies: () -> Unit,
    onExplorationMode: () -> Unit,
    onPublishSighting: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "BirdExplorer",
            style = MaterialTheme.typography.titleMedium
        )

        Image(
            painter = painterResource(id = R.drawable.robin_home),
            contentDescription = "European robin",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
        )

        Text(
            text = "Discover the birds around you",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Explore bird observations in Spain.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )

        // Botones de navegación
        HomeOption(
            title = "Explore near me",
            description = "Use your location to view nearby records.",
            onClick = onExploreNearMe
        )

        HomeOption(
            title = "Explore by area",
            description = "Select a region or province in Spain.",
            onClick = onExploreByArea
        )

        HomeOption(
            title = "Search for a species",
            onClick = onSearchSpecies
        )

        HomeOption(
            title = "Exploration mode",
            description = "Track your walk and steps.",
            onClick = onExplorationMode
        )

        HomeOption(
            title = "Publish sighting",
            description = "Share your bird sightings with other users.",
            onClick = onPublishSighting
        )
    }
}

@Composable
fun HomeOption(
    title: String,
    description: String = "",
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 8.dp
            )
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )

            if (description.isNotEmpty()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}


@Composable
fun ExplorationModeScreen(
    viewModel: ExplorationViewModel, // NUEVO
    onBack: () -> Unit,
    onPublishSighting: () -> Unit
) {
    // NUEVO: observar el estado del ViewModel
    val uiState by viewModel.uiState.collectAsState()

    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startExploration()
        } else {
            Toast.makeText(
                context,
                "Physical activity permission denied",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Exploration Mode",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = "Time: %02d:%02d:%02d".format(
                        uiState.elapsedSeconds / 3600,
                        (uiState.elapsedSeconds % 3600) / 60,
                        uiState.elapsedSeconds % 60
                    ),
                    style = MaterialTheme.typography.headlineMedium
                )

                Text(
                    text = "Steps: ${uiState.steps}",
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onPublishSighting,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Publish Sighting")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                if (ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACTIVITY_RECOGNITION
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    viewModel.startExploration()
                                } else {
                                    permissionLauncher.launch(
                                        Manifest.permission.ACTIVITY_RECOGNITION
                                    )
                                }
                            } else {
                                viewModel.startExploration()
                            }
                        }
                    ) {
                        Text("Start")
                    }

                    Button(
                        onClick = {
                            // Solo mostramos resumen si la exploración estaba en marcha
                            if (uiState.isRunning) {
                                // 1. Capturamos los datos del resumen ANTES de reiniciar a 0
                                val seconds = uiState.elapsedSeconds
                                val steps = uiState.steps

                                val timeFormatted = "%02d:%02d:%02d".format(
                                    seconds / 3600,
                                    (seconds % 3600) / 60,
                                    seconds % 60
                                )

                                // 2. Detenemos la exploración y ponemos variables a cero
                                viewModel.finishExploration()

                                // 3. Mostramos el Toast con el resumen final
                                Toast.makeText(
                                    context,
                                    "Exploration summary:\nTime: $timeFormatted | Steps: $steps",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    ) {
                        Text("Finish")
                    }
                }
            }
        }

        OutlinedButton(onClick = onBack) {
            Text("Back")
        }
    }
}

@Composable
fun SightingsScreen(
    onPublish: () -> Unit,
    onViewSightings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Community Sightings",
            style = MaterialTheme.typography.titleLarge
        )

        Button(
            onClick = onPublish,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Publish a Sighting")
        }

        Button(
            onClick = onViewSightings,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("View Sightings")
        }
    }
}


@Composable
fun PublishSightingScreen(
    onBack: () -> Unit,
    onPublish: (String, String, String) -> Unit
) {
    var species by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Publish a Sighting",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // Nombre de la especie
        OutlinedTextField(
            value = species,
            onValueChange = { species = it },
            label = { Text("Bird species") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Ubicación del avistamiento
        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Location") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Notas adicionales
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes (optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        // Botón de publicación
        Button(
            onClick = {
                onPublish(species, location, notes)
            },
            enabled = species.isNotBlank() && location.isNotBlank()
        ) {
            Text("Publish")
        }

        OutlinedButton(onClick = onBack) {
            Text("Back")
        }
    }
}


@Composable
fun ViewSightingsScreen(
    onBack: () -> Unit,
    sightings: List<Sighting>
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Community Sightings",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sightings) { sighting ->
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${sighting.species} - ${sighting.location}",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        OutlinedButton(onClick = onBack) {
            Text("Back")
        }
    }
}



//Plantillas temporales
@Composable
fun PlaceholderScreen(
    screenText: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("You are in $screenText")

        Button(onClick = onBack) {
            Text("Back")
        }
    }
}