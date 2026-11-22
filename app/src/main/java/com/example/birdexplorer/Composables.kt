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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextFieldDefaults


@Composable
fun HomeScreen(
    onExploreNearMe: () -> Unit,
    onExploreByArea: () -> Unit,
    onSearchSpecies: () -> Unit,
    onExplorationMode: () -> Unit,
    onPublishSighting: () -> Unit
) {
    val isPortrait = (
            LocalConfiguration.current.orientation ==
                    Configuration.ORIENTATION_PORTRAIT
            )

    if (isPortrait) {
        // Diseño vertical (portrait)
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

    } else {
        // Diseño horizontal (landscape), sin scroll
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Izquierda: título e imagen
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
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
                        .clip(RoundedCornerShape(10.dp))
                )

                Text(
                    text = "Discover the birds around you",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Explore bird observations in Spain.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // Derecha: cinco botones, sin scroll
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
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
    viewModel: ExplorationViewModel,
    onBack: () -> Unit,
    onPublishSighting: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val isPortrait = (
            LocalConfiguration.current.orientation ==
                    Configuration.ORIENTATION_PORTRAIT
            )

    // Colores de Exploration Mode
    val green = Color(0xFF245C3C)
    val background = Color(0xFFF6F8F2)

    // Solicitud del permiso de actividad física
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

    // START: iniciar temporizador y sensor de pasos
    val startAction: () -> Unit = {
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

    // FINISH: detener exploración y mostrar resumen
    val finishAction: () -> Unit = {
        if (uiState.isRunning) {
            val seconds = uiState.elapsedSeconds
            val steps = uiState.steps

            val timeFormatted = "%02d:%02d:%02d".format(
                seconds / 3600,
                (seconds % 3600) / 60,
                seconds % 60
            )

            viewModel.finishExploration()

            Toast.makeText(
                context,
                "Exploration summary:\nTime: $timeFormatted | Steps: $steps",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Formato del tiempo
    val timeText = "%02d:%02d:%02d".format(
        uiState.elapsedSeconds / 3600,
        (uiState.elapsedSeconds % 3600) / 60,
        uiState.elapsedSeconds % 60
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(if (isPortrait) 16.dp else 10.dp),
        verticalArrangement = Arrangement.spacedBy(
            if (isPortrait) 16.dp else 8.dp
        )
    ) {
        // Cabecera BirdExplorer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(green)
                .padding(
                    horizontal = 16.dp,
                    vertical = if (isPortrait) 16.dp else 9.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🐦",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "BirdExplorer",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        if (isPortrait) {
            // PORTRAIT: estadísticas encima de los controles
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Exploration Mode",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = if (uiState.isRunning)
                            "Exploration in progress"
                        else
                            "Ready to explore",
                        style = MaterialTheme.typography.bodyMedium,
                        color = green
                    )
                }

                ExplorationStatsCard(
                    timeText = timeText,
                    steps = uiState.steps
                )

                ExplorationControlButtons(
                    isRunning = uiState.isRunning,
                    onStart = startAction,
                    onFinish = finishAction,
                    onPublish = onPublishSighting,
                    onBack = onBack
                )
            }

        } else {
            // LANDSCAPE: estadísticas a la izquierda,
            // controles a la derecha, sin scroll
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Exploration Mode",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    ExplorationStatsCard(
                        timeText = timeText,
                        steps = uiState.steps,
                        modifier = Modifier.weight(1f),
                        compact = true
                    )
                }

                ExplorationControlButtons(
                    isRunning = uiState.isRunning,
                    onStart = startAction,
                    onFinish = finishAction,
                    onPublish = onPublishSighting,
                    onBack = onBack,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    compact = true
                )
            }
        }
    }
}


// Tarjeta reutilizable para tiempo y pasos
@Composable
private fun ExplorationStatsCard(
    timeText: String,
    steps: Int,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val green = Color(0xFF245C3C)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (compact) 12.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(
                if (compact) 12.dp else 20.dp
            )
        ) {
            // Tiempo
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Time",
                    style = MaterialTheme.typography.titleMedium,
                    color = green
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = timeText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider(
                color = Color(0xFFE0E7DD)
            )

            // Pasos
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Steps",
                    style = MaterialTheme.typography.titleMedium,
                    color = green
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "$steps",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


@Composable
private fun ExplorationControlButtons(
    isRunning: Boolean,
    onStart: () -> Unit,
    onFinish: () -> Unit,
    onPublish: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val green = Color(0xFF245C3C)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(
            if (compact) 6.dp else 12.dp
        )
    ) {
        // Solo se puede publicar durante una exploración
        OutlinedButton(
            onClick = onPublish,
            enabled = isRunning,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = green
            )
        ) {
            Text("＋  Publish Sighting")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onStart,
                enabled = !isRunning,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = green
                )
            ) {
                Text("Start")
            }

            Button(
                onClick = onFinish,
                enabled = isRunning,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = green
                )
            ) {
                Text("Finish")
            }
        }

        OutlinedButton(
            onClick = onBack,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = green
            )
        ) {
            Text("← Back")
        }
    }
}


@Composable
fun SightingsScreen(
    onPublish: () -> Unit,
    onViewSightings: () -> Unit
) {
    val isPortrait = (
            LocalConfiguration.current.orientation ==
                    Configuration.ORIENTATION_PORTRAIT
            )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Community Sightings",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        if (isPortrait) {
            // Portrait: tarjetas una debajo de otra
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SightingOption(
                    imageRes = R.drawable.publish_sighting,
                    title = "Publish a Sighting",
                    onClick = onPublish,
                    modifier = Modifier.weight(1f)
                )

                SightingOption(
                    imageRes = R.drawable.view_sightings,
                    title = "View Sightings",
                    onClick = onViewSightings,
                    modifier = Modifier.weight(1f)
                )
            }

        } else {
            // Landscape: tarjetas una al lado de otra
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SightingOption(
                    imageRes = R.drawable.publish_sighting,
                    title = "Publish a Sighting",
                    onClick = onPublish,
                    modifier = Modifier.weight(1f)
                )

                SightingOption(
                    imageRes = R.drawable.view_sightings,
                    title = "View Sightings",
                    onClick = onViewSightings,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}


@Composable
fun SightingOption(
    imageRes: Int,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
            )

            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(title)
            }
        }
    }
}







@Composable
fun PublishSightingScreen(
    onBack: () -> Unit,
    onPublish: (String, String, String, (Boolean) -> Unit) -> Unit
) {
    var species by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }

    val context = LocalContext.current

    val isPortrait = (
            LocalConfiguration.current.orientation ==
                    Configuration.ORIENTATION_PORTRAIT
            )

    val green = Color(0xFF245C3C)
    val background = Color(0xFFF6F8F2)

    // Acción compartida por los botones Publish de ambas orientaciones
    val publishAction: () -> Unit = {
        onPublish(species, location, notes) { success ->
            if (success) {
                Toast.makeText(
                    context,
                    "Sighting published!",
                    Toast.LENGTH_SHORT
                ).show()

                species = ""
                location = ""
                notes = ""
            } else {
                Toast.makeText(
                    context,
                    "Failed to publish sighting",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(if (isPortrait) 16.dp else 10.dp),
        verticalArrangement = Arrangement.spacedBy(
            if (isPortrait) 16.dp else 8.dp
        )
    ) {
        // Cabecera BirdExplorer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(green)
                .padding(
                    horizontal = 16.dp,
                    vertical = if (isPortrait) 16.dp else 9.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🐦",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "BirdExplorer",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Título
        Text(
            text = "Publish a Sighting",
            style = if (isPortrait)
                MaterialTheme.typography.headlineSmall
            else
                MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        if (isPortrait) {
            // PORTRAIT
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = species,
                    onValueChange = { species = it },
                    label = { Text("Bird species") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = green,
                        focusedLabelColor = green,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = green,
                        focusedLabelColor = green,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = green,
                        focusedLabelColor = green,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                // Publicación MQTT
                OutlinedButton(
                    onClick = publishAction,
                    enabled = species.isNotBlank() &&
                            location.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = green
                    )
                ) {
                    Text("＋  Publish Sighting")
                }

                OutlinedButton(
                    onClick = onBack,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = green
                    )
                ) {
                    Text("← Back")
                }
            }

        } else {
            // LANDSCAPE
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Izquierda: especie, ubicación y botones
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = species,
                        onValueChange = { species = it },
                        label = { Text("Bird species") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = green,
                            focusedLabelColor = green,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = green,
                            focusedLabelColor = green,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = publishAction,
                            enabled = species.isNotBlank() &&
                                    location.isNotBlank(),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = green
                            )
                        ) {
                            Text("＋  Publish")
                        }

                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = green
                            )
                        ) {
                            Text("← Back")
                        }
                    }
                }

                // Derecha: notas opcionales
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = green,
                        focusedLabelColor = green,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
            }
        }
    }
}


@Composable
fun ViewSightingsScreen(
    onBack: () -> Unit,
    sightings: List<Sighting>
) {
    val isPortrait = (
            LocalConfiguration.current.orientation ==
                    Configuration.ORIENTATION_PORTRAIT
            )

    val green = Color(0xFF245C3C)
    val background = Color(0xFFF6F8F2)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(if (isPortrait) 16.dp else 10.dp),
        verticalArrangement = Arrangement.spacedBy(
            if (isPortrait) 16.dp else 8.dp
        )
    ) {
        // Cabecera BirdExplorer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(green)
                .padding(
                    horizontal = 16.dp,
                    vertical = if (isPortrait) 16.dp else 9.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🐦",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "BirdExplorer",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        if (isPortrait) {
            // PORTRAIT
            Text(
                text = "Community Sightings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Recent sightings from the community",
                style = MaterialTheme.typography.bodyMedium,
                color = green
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sightings) { sighting ->
                    SightingCard(sighting)
                }
            }

            OutlinedButton(
                onClick = onBack,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = green
                )
            ) {
                Text("← Back")
            }

        } else {
            // LANDSCAPE
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Izquierda: título y navegación
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Community Sightings",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Recent sightings from the community",
                            style = MaterialTheme.typography.bodySmall,
                            color = green
                        )
                    }

                    OutlinedButton(
                        onClick = onBack,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = green
                        )
                    ) {
                        Text("← Back")
                    }
                }

                // Derecha: lista con scroll
                LazyColumn(
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sightings) { sighting ->
                        SightingCard(sighting)
                    }
                }
            }
        }
    }
}

// Tarjeta reutilizable para cada avistamiento
@Composable
private fun SightingCard(sighting: Sighting) {
    val green = Color(0xFF245C3C)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Especie
            Text(
                text = sighting.species,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = green
            )

            // Ubicación
            Text(
                text = "📍 ${sighting.location}",
                style = MaterialTheme.typography.bodyMedium
            )

            // Notas, solo si existen
            if (sighting.notes.isNotBlank()) {
                Text(
                    text = sighting.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
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