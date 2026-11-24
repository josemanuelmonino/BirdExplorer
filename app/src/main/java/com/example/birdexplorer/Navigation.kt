package com.example.birdexplorer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController


enum class Screens { Home, ExploreNearMe, ExploreByArea, SearchSpecies, ExplorationMode, Sightings, PublishSighting, ViewSightings}

@Composable
fun BirdExplorerApp(modifier: Modifier = Modifier, mqttViewModel: MqttViewModel) {

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

        composable(Screens.SearchSpecies.name) {
            val birdViewModel: BirdViewModel = viewModel()
            SearchSpeciesScreen(
                viewModel = birdViewModel
            )
        }
        composable(Screens.ExploreByArea.name) {
            val birdViewModel: BirdViewModel = viewModel()
            ExploreByAreaScreen(
                viewModel = birdViewModel
            )
        }

        composable(Screens.ExplorationMode.name) {

            val viewModel: ExplorationViewModel = viewModel()

            ExplorationModeScreen(
                viewModel = viewModel,
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
                onBack = { navController.popBackStack() },
                onPublish = { species, location, notes, onResult ->
                    mqttViewModel.publishSighting(
                        species = species,
                        location = location,
                        notes = notes,
                        onResult = onResult
                    )
                }
            )
        }

        composable(Screens.ViewSightings.name) {
            ViewSightingsScreen(
                onBack = { navController.popBackStack() },
                sightings = mqttUiState.sightings
            )
        }
    }
}