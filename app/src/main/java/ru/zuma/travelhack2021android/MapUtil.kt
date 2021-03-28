package ru.zuma.travelhack2021android

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.startForegroundService
import com.here.android.mpa.common.GeoBoundingBox
import com.here.android.mpa.common.GeoCoordinate
import com.here.android.mpa.common.GeoPolyline
import com.here.android.mpa.common.GeoPosition
import com.here.android.mpa.guidance.NavigationManager
import com.here.android.mpa.guidance.NavigationManager.MapUpdateMode
import com.here.android.mpa.guidance.NavigationManager.NavigationManagerEventListener
import com.here.android.mpa.mapping.*
import com.here.android.mpa.mapping.Map
import com.here.android.mpa.routing.*
import java.lang.ref.WeakReference


private lateinit var context: Context
private lateinit var mapFragment: AndroidXMapFragment
private lateinit var navigationManager: NavigationManager
var onNavigationPositionChanged: ((GeoPosition) -> Unit)? = null
private var foregroundServiceIntent: Intent? = null

fun initMapUtil(cntxt: Context, mapFrgmnt: AndroidXMapFragment, navigationMngr: NavigationManager) {
    context = cntxt
    mapFragment = mapFrgmnt
    navigationManager = navigationMngr
}

fun displayRoute(map: Map, route: IziTravelRoute) {
    for (point in route.points) {
        val marker = MapMarker(point)
        map.addMapObject(marker)
    }

    val polyline = MapPolyline(GeoPolyline(route.points))
    polyline.lineColor = Color.argb(150, (Math.random() * 255).toInt(), (Math.random() * 255).toInt(), (Math.random() * 255).toInt())
    polyline.lineWidth = 20
    map.addMapObject(polyline)

}

fun calculateDisplayRoute(map: Map, route: IziTravelRoute, selfCoordinate: GeoCoordinate? = null) {
    val points = if (selfCoordinate != null) {
        ArrayList(route.points).apply {
            add(0, selfCoordinate)
        }
    } else {
        route.points
    }

    val routeOptions = RouteOptions()
    routeOptions.transportMode = RouteOptions.TransportMode.PEDESTRIAN
    routeOptions.routeType = RouteOptions.Type.FASTEST

    val coreRouter = CoreRouter()
    coreRouter.calculateRoute(points, routeOptions, object : CoreRouter.Listener {
        override fun onCalculateRouteFinished(
            list: List<RouteResult>,
            routingError: RoutingError
        ) {
            if (routingError == RoutingError.NONE) {
                val resultRoute = list[0].route

                val startIdx = if (selfCoordinate == null) 0 else 1
                for (i in startIdx until points.size) {
                    val marker = MapMarker(points[i])
//                    marker.setLabelText("ang", (i + 1 - startIdx).toString())
                    map.addMapObject(marker)
                }

                val mapRoute = MapRoute(resultRoute)
                map.addMapObject(mapRoute)

                mapRoute.isManeuverNumberVisible = true

                map.zoomTo(resultRoute.boundingBox!!, Map.Animation.NONE, Map.MOVE_PRESERVE_ORIENTATION)

                startNavigation(map, resultRoute)
            }
        }

        override fun onProgress(i: Int) {}
    })
}

fun startNavigation(map: Map, route: Route) {
    /* Configure Navigation manager to launch navigation on current map */
    navigationManager.setMap(map)
    // show position indicator
    // note, it is also possible to change icon for the position indicator
    mapFragment.positionIndicator!!.isVisible = true

    /*
         * Start the turn-by-turn navigation.Please note if the transport mode of the passed-in
         * route is pedestrian, the NavigationManager automatically triggers the guidance which is
         * suitable for walking. Simulation and tracking modes can also be launched at this moment
         * by calling either simulate() or startTracking()
         */

    /* Choose navigation modes between real time navigation and simulation */
    val alertDialogBuilder = AlertDialog.Builder(context)
    alertDialogBuilder.setTitle("Navigation")
    alertDialogBuilder.setMessage("Choose Mode")
    alertDialogBuilder.setNegativeButton("Navigation",
        DialogInterface.OnClickListener { dialoginterface, i ->
            navigationManager.startNavigation(route)
//            map.tilt = 60f
            foregroundServiceIntent = Intent(context, context.javaClass).apply {
                startForegroundService(context, this)
            }
        })
    alertDialogBuilder.setPositiveButton("Simulation",
        DialogInterface.OnClickListener { dialoginterface, i ->
            navigationManager.simulate(route, 4) //Simualtion speed is set to 60 m/s
//            map.setTilt(60f)
            foregroundServiceIntent = Intent(context, context.javaClass).apply {
                startForegroundService(context, this)
            }
        })
    val alertDialog = alertDialogBuilder.create()
    alertDialog.show()

    /*
     * Set the map update mode to ROADVIEW.This will enable the automatic map movement based on
     * the current location.If user gestures are expected during the navigation, it's
     * recommended to set the map update mode to NONE first. Other supported update mode can be
     * found in HERE Mobile SDK for Android (Premium) API doc
     */
    navigationManager.mapUpdateMode = MapUpdateMode.ROADVIEW

    /*
     * NavigationManager contains a number of listeners which we can use to monitor the
     * navigation status and getting relevant instructions.In this example, we will add 2
     * listeners for demo purpose,please refer to HERE Android SDK API documentation for details
     */
    addNavigationListeners()
}

fun addNavigationListeners() {

    /*
     * Register a NavigationManagerEventListener to monitor the status change on
     * NavigationManager
     */
    navigationManager.addNavigationManagerEventListener(
        WeakReference<NavigationManagerEventListener>(
            navigationManagerEventListener
        )
    )

    /* Register a PositionListener to monitor the position updates */
    navigationManager.addPositionListener(
        WeakReference<NavigationManager.PositionListener>(object : NavigationManager.PositionListener() {
            override fun onPositionUpdated(p0: GeoPosition) {
                onNavigationPositionChanged?.invoke(p0)
            }
        })
    )
}

private val navigationManagerEventListener: NavigationManagerEventListener =
    object : NavigationManagerEventListener() {
        override fun onRunningStateChanged() {
            Toast.makeText(context, "Running state changed", Toast.LENGTH_SHORT).show()
        }

        override fun onNavigationModeChanged() {
            Toast.makeText(context, "Navigation mode changed", Toast.LENGTH_SHORT).show()
        }

        override fun onEnded(navigationMode: NavigationManager.NavigationMode) {
            Toast.makeText(context, "$navigationMode was ended", Toast.LENGTH_SHORT)
                .show()
            context.stopService(foregroundServiceIntent)
            foregroundServiceIntent = null
        }

        override fun onMapUpdateModeChanged(mapUpdateMode: MapUpdateMode) {
            Toast.makeText(
                context, "Map update mode is changed to $mapUpdateMode",
                Toast.LENGTH_SHORT
            ).show()
        }

        override fun onRouteUpdated(route: Route) {
            Toast.makeText(context, "Route updated", Toast.LENGTH_SHORT).show()
        }

        override fun onCountryInfo(s: String, s1: String) {
            Toast.makeText(
                context, "Country info updated from $s to $s1",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

fun findPointInBounds(points: List<GeoCoordinate>, center: GeoCoordinate, radius: Float) : Int? {
    val boundingBox = GeoBoundingBox(center, radius * 2, radius * 2)
    val idx = points.indexOfFirst { boundingBox.contains(it) }
    return if (idx == -1) null else idx
}