package com.example.birdexplorer.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

// By default, centered around UPM Campus Sur
val DEFAULT_CENTER = GeoPoint(40.3893, -3.6286)
const val DEFAULT_ZOOM = 16.0

@Composable
fun OsmMap(
    modifier: Modifier = Modifier,
    center: GeoPoint = DEFAULT_CENTER,
    zoom: Double = DEFAULT_ZOOM,
    tileSource: ITileSource = TileSourceFactory.MAPNIK
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
        }, //to avoid resetting tile source with every change in the UI
        modifier = modifier.fillMaxSize()
    )
}