package com.example.birdexplorer.ui

import android.R.attr.onClick
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.MapTileIndex

//osmdroid's USGS_SAT for satellite view belongs to the US, only had satellite view of places inside the US
//using Esri World Imagery we can use satellite view for Spain
val ESRI_SATELLITE: ITileSource = object : OnlineTileSourceBase(
    "EsriWorldImagery",
    0, 19, 256, ".jpg",
    arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/")
) {
    override fun getTileURLString(pTileIndex: Long): String {
        return baseUrl +
                MapTileIndex.getZoom(pTileIndex) + "/" +
                MapTileIndex.getY(pTileIndex) + "/" +
                MapTileIndex.getX(pTileIndex)
    }
}

enum class MapStyle(val label: String, val tileSource: ITileSource) {
    STANDARD("Standard", TileSourceFactory.MAPNIK),
    TOPO("Topographic", TileSourceFactory.OpenTopo),
    SATELLITE("Satellite", ESRI_SATELLITE) //USGS_SAT belongs to the US, only had
                                                            //satellite view of places inside the US
}

@Composable
fun MapScreen(onBack: () -> Unit) {
    var selectedStyle by rememberSaveable { mutableStateOf(MapStyle.STANDARD) } //standard style by default
    var menuExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()){
        OsmMap(tileSource = selectedStyle.tileSource)

        Button(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Text("Back")
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            FilledTonalButton(onClick = { menuExpanded = true }) {
                Text("${selectedStyle.label} map")
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = {menuExpanded = false}
            ) {
                MapStyle.entries.forEach { style ->
                    DropdownMenuItem(
                        text = { Text(style.label) },
                        onClick = {
                            selectedStyle = style
                            menuExpanded = false
                        }
                    )
                }
            }
        }
    }
}