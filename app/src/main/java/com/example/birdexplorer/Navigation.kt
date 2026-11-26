
package com.example.birdexplorer

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.birdexplorer.ui.MapViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.graphicsLayer

enum class Screens {
    Home,
    ExploreNearMe,
    ExploreByArea,
    SearchSpecies,
    ExplorationMode,
    Sightings,
    PublishSighting,
    ViewSightings
}

@Composable
fun BrokerDialog(
    currentUrl: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by rememberSaveable { mutableStateOf(currentUrl) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("MQTT broker") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("IP or URL (e.g. 192.168.1.50)") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(text); onDismiss() }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun BirdExplorerApp(
    modifier: Modifier = Modifier,
    mqttViewModel: MqttViewModel,
    explorationViewModel: ExplorationViewModel
) {
    val navController = rememberNavController()
    var colorFilterEnabled by rememberSaveable { mutableStateOf(false) }
    val mqttUiState by mqttViewModel.uiState.collectAsState()
    val brokerUrl by mqttViewModel.brokerUrl.collectAsState()
    var showBrokerDialog by rememberSaveable { mutableStateOf(false) }

    if (showBrokerDialog) {
        BrokerDialog(
            currentUrl = brokerUrl,
            onDismiss = { showBrokerDialog = false },
            onSave = { mqttViewModel.changeBroker(it) }
        )
    }

    NavHost(
        navController = navController,
        startDestination = Screens.Home.name,
        modifier = modifier.graphicsLayer {
            colorFilter = if (colorFilterEnabled) {
                ColorFilters.deuteranopiaFilter
            } else {
                null
            }
        }
    ) {

        composable(Screens.Home.name) {
            HomeScreen(
                onExploreNearMe = {
                    navController.navigate(Screens.ExploreNearMe.name)
                },
                onExploreByArea = {
                    navController.navigate(Screens.ExploreByArea.name)
                },
                onSearchSpecies = {
                    navController.navigate(Screens.SearchSpecies.name)
                },
                onExplorationMode = {
                    navController.navigate(Screens.ExplorationMode.name)
                },
                onPublishSighting = {
                    navController.navigate(Screens.Sightings.name)
                },
                colorFilterEnabled = colorFilterEnabled,
                onColorFilterChange = { colorFilterEnabled = it },
                onOpenBrokerSettings = { showBrokerDialog = true }
            )
        }

        composable(Screens.ExploreNearMe.name) {
            val birdViewModel: BirdViewModel = viewModel()
            val mapViewModel: MapViewModel = viewModel()

            ExploreNearMeScreen(
                viewModel = birdViewModel,
                mapViewModel = mapViewModel,
                mqttSightings = mqttUiState.sightings
            )
        }

        composable(Screens.ExploreByArea.name) {
            val birdViewModel: BirdViewModel = viewModel()
            val mapViewModel: MapViewModel = viewModel()

            ExploreByAreaScreen(
                viewModel = birdViewModel,
                mapViewModel = mapViewModel,
                mqttSightings = mqttUiState.sightings
            )
        }

        composable(Screens.SearchSpecies.name) {
            val birdViewModel: BirdViewModel = viewModel()

            SearchSpeciesScreen(
                viewModel = birdViewModel
            )
        }

        composable(Screens.ExplorationMode.name) {
            ExplorationModeScreen(
                viewModel = explorationViewModel,
                onBack = {
                    navController.popBackStack()
                },
                onPublishSighting = {
                    navController.navigate(Screens.PublishSighting.name)
                }
            )
        }


        composable(Screens.Sightings.name) {
            SightingsScreen(
                onPublish = {
                    navController.navigate(Screens.PublishSighting.name)
                },
                onViewSightings = {
                    navController.navigate(Screens.ViewSightings.name)
                }
            )
        }

        composable(Screens.PublishSighting.name) {
            PublishSightingScreen(
                onBack = {
                    navController.popBackStack()
                },
                onPublish = { species, location, notes, lat, lon, onResult ->
                    mqttViewModel.publishSighting(
                        species = species,
                        location = location,
                        notes = notes,
                        lat = lat,
                        lon = lon,
                        onResult = onResult
                    )
                }
            )
        }

        composable(Screens.ViewSightings.name) {
            ViewSightingsScreen(
                onBack = {
                    navController.popBackStack()
                },
                sightings = mqttUiState.sightings
            )
        }
    }
}