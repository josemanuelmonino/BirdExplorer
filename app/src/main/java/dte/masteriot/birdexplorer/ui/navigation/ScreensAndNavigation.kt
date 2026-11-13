package dte.masteriot.birdexplorer.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dte.masteriot.birdexplorer.R
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun HomeScreen(
    onNearMeClick: () -> Unit,
    onByAreaClick: () -> Unit,
    onSearchClick: () -> Unit,
    onExplorationClick: () -> Unit,
    onPublishClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.home_title),
            style = MaterialTheme.typography.headlineLarge
        )
        HomeButton(R.string.home_explore_near, onNearMeClick)
        HomeButton(R.string.home_explore_area, onByAreaClick)
        HomeButton(R.string.home_search_species, onSearchClick)
        HomeButton(R.string.home_exploration_mode, onExplorationClick)
        HomeButton(R.string.home_publish_sighting, onPublishClick)
    }
}

@Composable
private fun HomeButton(textRes: Int, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
    ) {
        Text(
            text = stringResource(textRes),
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
fun BirdExplorerNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()   // el conductor

    NavHost(                                       // el mapa
        navController = navController,
        startDestination = Screens.Home.name,
        modifier = modifier
    ) {
        composable(Screens.Home.name) {
            HomeScreen(
                onNearMeClick = { navController.navigate(Screens.NearMe.name) },
                onByAreaClick = { navController.navigate(Screens.ByArea.name) },
                onSearchClick = { navController.navigate(Screens.SearchSpecies.name) },
                onExplorationClick = { navController.navigate(Screens.ExplorationMode.name) },
                onPublishClick = { navController.navigate(Screens.PublishSighting.name) }
            )
        }
        composable(Screens.NearMe.name) { Text("Explorar cerca de mí") }
        composable(Screens.ByArea.name) { Text("Explorar por zona") }
        composable(Screens.SearchSpecies.name) { Text("Buscar especie") }
        composable(Screens.ExplorationMode.name) { Text("Modo exploración") }
        composable(Screens.PublishSighting.name) { Text("Publicar avistamiento") }
    }
}