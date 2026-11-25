
package com.example.birdexplorer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.birdexplorer.ui.MapViewModel

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
fun BirdExplorerApp(
    modifier: Modifier = Modifier,
    mqttViewModel: MqttViewModel
) {
    val navController = rememberNavController()
    val mqttUiState by mqttViewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Screens.Home.name,
        modifier = modifier
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
                }
            )
        }

        composable(Screens.ExploreNearMe.name) {
            val birdViewModel: BirdViewModel = viewModel()

            ExploreNearMeScreen(
                viewModel = birdViewModel
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
            val explorationViewModel: ExplorationViewModel = viewModel()

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
