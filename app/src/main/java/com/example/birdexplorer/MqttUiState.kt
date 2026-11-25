package com.example.birdexplorer

data class Sighting(
    val species: String,
    val location: String,
    val notes: String,
    val timestamp: Long,
    val lat: Double? = null,
    val lon: Double? = null,
)

data class MqttUiState(
    val sightings: List<Sighting> = emptyList()
)