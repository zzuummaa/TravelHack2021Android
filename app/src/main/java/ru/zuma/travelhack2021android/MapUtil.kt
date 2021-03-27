package ru.zuma.travelhack2021android

import android.graphics.Color
import com.here.android.mpa.common.GeoPolyline
import com.here.android.mpa.mapping.*
import com.here.android.mpa.mapping.Map
import com.here.android.mpa.routing.CoreRouter
import com.here.android.mpa.routing.RouteOptions
import com.here.android.mpa.routing.RouteResult
import com.here.android.mpa.routing.RoutingError


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

fun calculateDisplayRoute(map: Map, route: IziTravelRoute) {
    val routeOptions = RouteOptions()
    routeOptions.transportMode = RouteOptions.TransportMode.PEDESTRIAN
    routeOptions.routeType = RouteOptions.Type.FASTEST

    val coreRouter = CoreRouter()
    coreRouter.calculateRoute(route.points, routeOptions, object : CoreRouter.Listener {
        override fun onCalculateRouteFinished(
            list: List<RouteResult>,
            routingError: RoutingError
        ) {
            if (routingError == RoutingError.NONE) {
                val resultRoute = list[0].route

                for (i in 0 until route.points.size) {
                    val marker = MapLabeledMarker(route.points[i])
                    marker.setLabelText("ang", (i+1).toString())
                    map.addMapObject(marker)
                }

                map.addMapObject(MapRoute(resultRoute))
            }
        }

        override fun onProgress(i: Int) {}
    })
}