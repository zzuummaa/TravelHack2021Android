package ru.zuma.travelhack2021android

import com.here.android.mpa.mapping.Map
import com.here.android.mpa.mapping.MapMarker

fun displayRoute(map: Map, route: MapRoute) {
    for (point in route.points) {
        val marker = MapMarker(point)
        map.addMapObject(marker)
    }
}