package com.example.birdexplorer

data class Sighting(
    val species: String,
    val location: String,
    val notes: String,
    val timestamp: Long
)

data class MqttUiState(
    val sightings: List<Sighting> = emptyList()
)