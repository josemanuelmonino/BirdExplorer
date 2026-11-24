package com.example.birdexplorer.ui

import android.graphics.Color
import android.widget.Toast
import com.example.birdexplorer.R
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.events.MapListener
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.util.BoundingBox
import java.util.Locale

// By default, centered around UPM Campus Sur
val DEFAULT_CENTER = GeoPoint(40.3893, -3.6286)

// Spain bounds including Canary and Balearic Islands (north, east, south, west)
val SPAIN_BOUNDS = BoundingBox(43.8, 4.4, 27.6, -18.2)
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
    recenterTrigger: Int = 0,
    tileSource: ITileSource = TileSourceFactory.MAPNIK,
    markers: List<MapMarkerItem> = emptyList(),
    userLocation: GeoPoint? = null,
    circleCenter: GeoPoint? = null,
    circleRadiusMeters: Double = 500.0,
    onMapLongPress: ((GeoPoint) -> Unit)? = null,
    onZoomChanged: ((Double) -> Unit)? = null
) {
    val context = LocalContext.current
    val currentOnLongPress by rememberUpdatedState(onMapLongPress)
    val currentOnZoomChanged by rememberUpdatedState(onZoomChanged)

    val mapView = remember {
        MapView(context).apply {
            setTileSource(tileSource)
            setMultiTouchControls(true)      //zoom, other screen touching interactions
            setScrollableAreaLimitDouble(SPAIN_BOUNDS)
            doOnLayout {
                controller.setZoom(zoom)
                controller.setCenter(center)
            }

            addMapListener(object : MapListener {
                override fun onScroll(event: ScrollEvent?): Boolean = false

                override fun onZoom(event: ZoomEvent?): Boolean {
                    event?.zoomLevel?.let { currentOnZoomChanged?.invoke(it)}
                    return false
                }
            })
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

    // re-recenter camera explicitly when requested (clicking "My location")
    LaunchedEffect(recenterTrigger) {
        if(recenterTrigger > 0){
            mapView.doOnLayout {
                mapView.controller.setZoom(zoom)
                mapView.controller.animateTo(center)
            }
        }
    }

    AndroidView(
        factory = { mapView },
        update = { view ->
            if (view.tileProvider.tileSource != tileSource) {
                view.setTileSource(tileSource)
            }

            // Touch events receiver
            view.overlays.removeAll { it is MapEventsOverlay}
            val mapEventsReceiver = object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                    Toast.makeText(
                        context,
                        String.format(Locale.US, "Lat: %.4f, Lon: %.4f", p.latitude, p.longitude),
                        Toast.LENGTH_SHORT
                    ).show()
                    return true
                }

                override fun longPressHelper(p: GeoPoint): Boolean {
                    currentOnLongPress?.invoke(p)
                    return true
                }
            }
            view.overlays.add(MapEventsOverlay(mapEventsReceiver))

            // Search radius circle
            view.overlays.removeAll { it is Polygon}
            if (circleCenter != null && circleRadiusMeters > 0) {
                val searchCircle = Polygon().apply {
                    points = Polygon.pointsAsCircle(circleCenter, circleRadiusMeters)
                    outlinePaint.color = Color.rgb(0, 121, 107)
                    outlinePaint.strokeWidth = 3f
                    fillPaint.color = Color.argb(30, 0, 121, 107)
                }
                view.overlays.add(0, searchCircle) //at index 0 so markers remain on top
            }

            // Remove ONLY old markers (and not the overlay, polygons or "My Location" marker
            // whenever something changes. If this wasn't done, they would be duplicated
            view.overlays.removeAll {
                it is Marker && (it.relatedObject is MapMarkerItem || it.relatedObject == "USER_LOCATION")
            }

            // Draw "My location" marker if GPS location is available
            if (userLocation != null) {
                val myLocationMarker = Marker(view).apply {
                    position = userLocation
                    title = "My Location"
                    relatedObject = "USER_LOCATION"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = ContextCompat.getDrawable(context, R.drawable.ic_my_location)
                }
                view.overlays.add(myLocationMarker)
            }

            //Draw bird sighting markers
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