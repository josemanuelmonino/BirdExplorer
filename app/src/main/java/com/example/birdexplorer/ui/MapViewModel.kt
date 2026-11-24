package com.example.birdexplorer.ui
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.osmdroid.util.GeoPoint

data class MapUiState(
    val selectedStyle: MapStyle = MapStyle.STANDARD,
    val searchCenter: GeoPoint = DEFAULT_CENTER,
    val zoom: Double = DEFAULT_ZOOM,
    val radiusMeters: Float = 500f,
    val userLocation: GeoPoint? = null,
    val recenterTrigger: Int = 0,
    val areaCenter: GeoPoint = DEFAULT_CENTER,
    val areaZoom: Double = 12.0,
    val areaRecenterTrigger: Int = 0,
    val apiMarkers: List<MapMarkerItem> = emptyList(),
    val mqttMarkers: List<MapMarkerItem> = emptyList()
) {
    // Combines API + MQTT sightings and filters by distance if radius filtering is active
    val visibleLocalMarkers: List<MapMarkerItem>
        get() = (apiMarkers + mqttMarkers).filter { marker ->
            val point = GeoPoint(marker.latitude, marker.longitude)
            searchCenter.distanceToAsDouble(point) <= radiusMeters
        }

    // All sightings without distance limit (used by Explore by Area)
    val allAreaMarkers: List<MapMarkerItem>
        get() = apiMarkers + mqttMarkers
}

class MapViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    fun onMapStyleSelected(style: MapStyle) {
        _uiState.update { it.copy(selectedStyle = style) }
    }

    fun onZoomChanged(newZoom: Double) {
        _uiState.update { it.copy(zoom = newZoom) }
    }

    fun onRadiusChanged(newRadius: Float) {
        _uiState.update { it.copy(radiusMeters = newRadius) }
    }

    fun onSearchCenterChanged(newCenter: GeoPoint, animateCamera: Boolean = false) {
        _uiState.update { state ->
            state.copy(
                searchCenter = newCenter,
                recenterTrigger = if (animateCamera) state.recenterTrigger + 1 else state.recenterTrigger
            )
        }
    }

    fun onDeviceLocationUpdated(location: GeoPoint) {
        _uiState.update { state ->
            state.copy(
                userLocation = location,
                searchCenter = location,
                recenterTrigger = state.recenterTrigger + 1
            )
        }
    }

    fun onAreaCenterSelected(newCenter: GeoPoint, zoom: Double? = null){
        _uiState.update { state ->
            state.copy(
                areaCenter = newCenter,
                areaZoom = zoom ?: state.areaZoom,
                areaRecenterTrigger = state.areaRecenterTrigger + 1
            )
        }
    }

    // INTEGRATION FOR API AND MQTT

    fun setApiObservations(observations: List<MapMarkerItem>) {
        _uiState.update { it.copy(apiMarkers = observations) }
    }

    fun addMqttSighting(sighting: MapMarkerItem) {
        _uiState.update { state ->
            state.copy(mqttMarkers = state.mqttMarkers + sighting)
        }
    }

    //SAMPLE MARKERS FOR TESTING
    companion object {
        private fun sampleApiMarkers() = listOf(
            MapMarkerItem(
                id = "api_101",
                title = "Gorrión común (Passer domesticus)",
                subtitle = "Campus Sur UPM, Madrid",
                latitude = 40.3898,
                longitude = -3.6282,
                type = MarkerType.API_OBSERVATION
            ),
            MapMarkerItem(
                id = "api_102",
                title = "Mirlo común (Turdus merula)",
                subtitle = "Parque Lineal del Manzanares",
                latitude = 40.3865,
                longitude = -3.6310,
                type = MarkerType.API_OBSERVATION
            )
        )

        private fun sampleMqttMarkers() = listOf(
            MapMarkerItem(
                id = "mqtt_1",
                title = "Cotorra argentina",
                subtitle = "Vistas 3 en los pinos junto a la cafetería",
                latitude = 40.3905,
                longitude = -3.6272,
                type = MarkerType.MQTT_USER_SIGHTING
            )
        )
    }
}