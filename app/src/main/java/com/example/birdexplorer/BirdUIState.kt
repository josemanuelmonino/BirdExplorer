package com.example.birdexplorer

import com.example.birdexplorer.data.BirdSighting
import com.example.birdexplorer.data.remote.Taxon

data class BirdUiState(
    val isLoading: Boolean = false,     // if a request is on going
    val sightings: List<BirdSighting> = emptyList(),   // what is drawn in the map or a list of sightingfs
    val speciesResults: List<Taxon> = emptyList(),     // results of the serch
    val selected: BirdSighting? = null,                // sighting shown in the detail sheet
    val error: String? = null,                         // connection or API problem to tell the user
    val info: String? = null                           // simple message for the user, like an empty result
)
