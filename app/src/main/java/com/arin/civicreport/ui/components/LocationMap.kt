package com.arin.civicreport.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

@Composable
fun LocationMap(
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(220.dp)
        .clip(RoundedCornerShape(14.dp)),
    onPick: ((Double, Double) -> Unit)? = null
) {
    val context = LocalContext.current
    val currentOnPick = rememberUpdatedState(onPick)

    val mapView = remember {
        runCatching {
            Configuration.getInstance().userAgentValue = context.packageName
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setUseDataConnection(true)
            controller.setZoom(16.0)
            controller.setCenter(GeoPoint(latitude, longitude))
            overlays.add(
                MapEventsOverlay(object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                        val point = p ?: return false
                        currentOnPick.value?.invoke(point.latitude, point.longitude)
                        return true
                    }

                    override fun longPressHelper(p: GeoPoint?): Boolean = false
                })
            )
        }
    }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose {
            runCatching { mapView.onPause() }
            runCatching { mapView.onDetach() }
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { map ->
            map.overlays.removeAll { it is Marker }
            val marker = Marker(map).apply {
                position = GeoPoint(latitude, longitude)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "Reported location"
                isDraggable = false
            }
            map.overlays.add(marker)
            map.controller.animateTo(GeoPoint(latitude, longitude))
            map.invalidate()
        }
    )
}
