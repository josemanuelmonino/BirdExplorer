package com.example.birdexplorer.ui

import com.example.birdexplorer.R
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

// By default, centered around UPM Campus Sur
val DEFAULT_CENTER = GeoPoint(40.3893, -3.6286)
const val DEFAULT_ZOOM = 16.0

// Both MarkerType and MapMarkerItem are defined in this file because any screen that calls
// OsmMap will use them together.
enum class MarkerType{
    API_OBSERVATION, //for standard observations obtained from iNaturalist API
    MQTT_USER_SIGHTING //for user sightings published through MQTT
}

data class MapMarkerItem(
    val id: String,
    val title: String, //species name
    val subtitle: String, //the place (API) or timestamp/notes (MQTT)
    val latitude: Double,
    val longitude: Double,
    val type: MarkerType
)

@Composable
fun OsmMap(
    modifier: Modifier = Modifier,
    center: GeoPoint = DEFAULT_CENTER,
    zoom: Double = DEFAULT_ZOOM,
    tileSource: ITileSource = TileSourceFactory.MAPNIK,
    markers: List<MapMarkerItem> = emptyList()
) {
    val context = LocalContext.current

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)      //zoom, other screen touching interactions
            controller.setZoom(zoom)
            controller.setCenter(center)
        }
    }

    // for MapView to follow the screen's lifecycle
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = { mapView },
        update = { view ->
            if (view.tileProvider.tileSource != tileSource) {
                view.setTileSource(tileSource)
            }

            // to remove ONLY old markers (and not the overlay, polygons or "My Location" marker
            // whenever something changes. If this wasn't done, they would be duplicated
            view.overlays.removeAll { it is Marker && it.relatedObject is MapMarkerItem}

            markers.forEach { item ->
                val marker = Marker(view).apply {
                    position = GeoPoint(item.latitude, item.longitude)
                    title = item.title
                    snippet = when (item.type) {
                        MarkerType.API_OBSERVATION -> "[API] ${item.subtitle}" //the place (API)
                        MarkerType.MQTT_USER_SIGHTING -> "[MQTT] ${item.subtitle}" //timestamp/notes (MQTT)
                    }
                    relatedObject = item
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                    // diff marker icon depending on if originated from API or MQTT
                    icon = when (item.type) {
                        MarkerType.API_OBSERVATION ->
                            ContextCompat.getDrawable(context, R.drawable.ic_marker_api)
                        MarkerType.MQTT_USER_SIGHTING ->
                            ContextCompat.getDrawable(context, R.drawable.ic_marker_mqtt)
                    }
                }
                view.overlays.add(marker)
            }

            view.invalidate()
        }, //to avoid resetting tile source with every change in the UI
        modifier = modifier.fillMaxSize()
    )
}