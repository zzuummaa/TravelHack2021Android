package ru.zuma.travelhack2021android

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.here.android.mpa.common.GeoCoordinate
import com.here.android.mpa.common.MapSettings.setDiskCacheRootPath
import com.here.android.mpa.common.OnEngineInitListener
import com.here.android.mpa.mapping.AndroidXMapFragment
import com.here.android.mpa.mapping.Map
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File


class MainActivity : FragmentActivity() {

    // map embedded in the map fragment
    private lateinit var map: Map

    // map fragment embedded in this activity
    private lateinit var mapFragment: AndroidXMapFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initialize()
    }

    private fun initialize() {

        // Search for the map fragment to finish setup by calling init().
        mapFragment =
            supportFragmentManager.findFragmentById(R.id.mapfragment) as AndroidXMapFragment

        // Set up disk map cache path for this application
        // Use path under your application folder for storing the disk cache
        setDiskCacheRootPath(
            getFilesDir().getPath() + File.separator + ".here-maps"
        )

        mapFragment.init(OnEngineInitListener { error ->
            if (error == OnEngineInitListener.Error.NONE) {
                // retrieve a reference of the map from the map fragment
                map = mapFragment.getMap()!!
                // Set the map center to the Vancouver region (no animation)
                map.setCenter(
                    GeoCoordinate(49.196261, -123.004773, 0.0),
                    Map.Animation.NONE
                )
                // Set the zoom level to the average between min and max
                map.setZoomLevel((map.getMaxZoomLevel() + map.getMinZoomLevel()) / 2)
            } else {
                Log.e(javaClass.simpleName, "Cannot initialize Map Fragment")
            }
        })

        btnFind.setOnClickListener {
            Log.d(javaClass.simpleName, "On submit")
            queryObject(
                etObjectQuery.text.toString(),
                { _, routes ->
                    // TODO
                    Log.d(javaClass.simpleName, "Success object query with ${routes.size} routes")
                    map.removeAllMapObjects()
                    for (route in routes) {
                        displayRoute(map, route)
                    }
                }
            )
        }
    }
}
