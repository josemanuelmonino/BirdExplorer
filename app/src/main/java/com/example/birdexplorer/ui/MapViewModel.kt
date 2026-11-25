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
    val cameraCenter: GeoPoint = DEFAULT_CENTER,
    val zoom: Double = DEFAULT_ZOOM,
    val radiusMeters: Float = 500f,
    val userLocation: GeoPoint? = null,
    val recenterTrigger: Int = 0,
    val areaCenter: GeoPoint = DEFAULT_CENTER,
    val areaZoom: Double = 12.0,
    val areaRecenterTrigger: Int = 0,
    val selectedMarkerId: String? = null,
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

    fun onSearchCenterChanged(
        newCenter: GeoPoint,
        animateCamera: Boolean = false,
        zoom: Double? = null
    ) {
        _uiState.update { state ->
            state.copy(
                searchCenter = newCenter,
                cameraCenter = newCenter,
                zoom = zoom ?: state.zoom,
                recenterTrigger = if (animateCamera) state.recenterTrigger + 1 else state.recenterTrigger
            )
        }
    }

    fun onDeviceLocationUpdated(location: GeoPoint) {
        _uiState.update { state ->
            state.copy(
                userLocation = location,
                searchCenter = location,
                cameraCenter = location,
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

    fun onMarkerSelected(id: String?) {
        _uiState.update { it.copy(selectedMarkerId = id) }
    }

    fun focusOnMarker(point: GeoPoint, zoom: Double) {
        _uiState.update { state ->
            state.copy(
                cameraCenter = point,
                zoom = zoom,
                recenterTrigger = state.recenterTrigger + 1
            )
        }
    }

    // INTEGRATION FOR API AND MQTT

    fun setApiObservations(observations: List<MapMarkerItem>) {
        _uiState.update { it.copy(apiMarkers = observations) }
    }

    fun setMqttSightings(sightings: List<MapMarkerItem>) {
        _uiState.update { it.copy(mqttMarkers = sightings)}
    }
}