package ru.zuma.travelhack2021android

import android.graphics.Color
import com.here.android.mpa.common.GeoPolyline
import com.here.android.mpa.mapping.Map
import com.here.android.mpa.mapping.MapMarker
import com.here.android.mpa.mapping.MapPolyline

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
