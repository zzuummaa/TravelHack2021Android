package ru.zuma.travelhack2021android

import android.Manifest
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.app.ActivityCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.FragmentActivity
import com.here.android.mpa.common.GeoCoordinate
import com.here.android.mpa.common.MapSettings.setDiskCacheRootPath
import com.here.android.mpa.common.OnEngineInitListener
import com.here.android.mpa.common.PositioningManager
import com.here.android.mpa.mapping.AndroidXMapFragment
import com.here.android.mpa.mapping.Map
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File


class MainActivity : FragmentActivity() {
    private var audioTriggerDistance: Float = 0.050f
    private val LOG_NAME = javaClass.simpleName
    private val TAG_CODE_PERMISSION_LOCATION = 1

    private lateinit var locationManager: LocationManager

    private var audioService = AudioService()

    private var routeTitles = arrayListOf<String>()
    private var routes = ArrayList<IziTravelRoute>()
    private var currentCoordinate: GeoCoordinate? = null
    private var pointsFull = arrayListOf<IziTravelPointFull>()

    // map embedded in the map fragment
    private var map: Map? = null

    // map fragment embedded in this activity
    private lateinit var mapFragment: AndroidXMapFragment

    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            Log.d(LOG_NAME, "Location changed: ${location.latitude},${location.longitude}")
            currentCoordinate = GeoCoordinate(location.latitude, location.longitude)
            if (spinnerRoutes.selectedItemPosition == -1) return
            map?.apply {
                this.removeAllMapObjects()
                calculateDisplayRoute(this, routes[spinnerRoutes.selectedItemPosition], currentCoordinate)
            }

            findPointInBounds(routes[spinnerRoutes.selectedItemPosition].points, currentCoordinate!!, audioTriggerDistance)?.apply {
                audioService.playMedia(pointsFull[spinnerRoutes.selectedItemPosition])
            }

        }

        override fun onProviderDisabled(provider: String) { }

        override fun onProviderEnabled(provider: String) {
            val location = try {
                locationManager.getLastKnownLocation(provider)
            } catch (e: SecurityException) {
                Log.e(LOG_NAME, "", e)
                null
            } ?: return

            Log.d(LOG_NAME, "Location provider enabled: ${location.latitude},${location.longitude}")
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle?) {
//            if (provider == LocationManager.GPS_PROVIDER) {
//                tvStatusGPS.setText("Status: $status")
//            } else if (provider == LocationManager.NETWORK_PROVIDER) {
//                tvStatusNet.setText("Status: $status")
//            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initialize()
    }

    private fun initialize() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val routesAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, routeTitles)
        routesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRoutes.adapter = routesAdapter

        spinnerRoutes.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                Log.d(LOG_NAME, "Choose route ${position}")
                runOnUiThread {
                    map?.apply {
                        removeAllMapObjects()
                        calculateDisplayRoute(this, routes[position], currentCoordinate)
                    }
                }

                val queryObjectFullCallback: QuerySuccessCallback<ArrayList<IziTravelPoint>> =  { _, responsePoints ->
                    Log.d(LOG_NAME, "Points query response with ${responsePoints.size} points")
                    runOnUiThread {
                        pointsFull = responsePoints.map { IziTravelPointFull(it.uuid) } as ArrayList
                    }
                    for (i in 0 until responsePoints.size) {
                        queryPointFull(
                            responsePoints[i].uuid, { _, pointFull ->
                            Log.d(LOG_NAME, "$pointFull")
                            runOnUiThread {
                                pointsFull[i] = pointFull
                            }
                        })
                    }
                }

                queryObjectFull(routes[position].uuid, queryObjectFullCallback)
            }

        }

        btnPlayAudio.setOnClickListener {
            if (pointsFull.isEmpty()) return@setOnClickListener
            audioService.playMedia(pointsFull[0])
        }

        btnStopAudio.setOnClickListener {
            audioService.stopMedia()
        }

        etTriggerDistance.doOnTextChanged { text, start, before, count ->
            Log.d(LOG_NAME, "Triger distance change to $text m.")
            audioTriggerDistance = text.toString().toInt() / 1000f
        }

        // Search for the map fragment to finish setup by calling init().
        mapFragment =
            supportFragmentManager.findFragmentById(R.id.mapfragment) as AndroidXMapFragment

        // Set up disk map cache path for this application
        // Use path under your application folder for storing the disk cache
        setDiskCacheRootPath(
            getFilesDir().getPath() + File.separator + ".here-maps"
        )

        mapFragment.init { error ->
            if (error == OnEngineInitListener.Error.NONE) {
                // retrieve a reference of the map from the map fragment
                map = mapFragment.getMap()?.apply {
                    // Set the map center to the Vancouver region (no animation)
                    setCenter(
                        GeoCoordinate(55.7522200, 37.6155600, 0.0),
                        Map.Animation.NONE
                    )
                    // Set the zoom level to the average between min and max
                    setZoomLevel((getMaxZoomLevel() + getMinZoomLevel()) / 2)
                }
                mapFragment.positionIndicator?.apply {
                    isVisible = true
                    isAccuracyIndicatorVisible = true
//                    accuracyIndicatorColor = Color.argb(255, 0, 0, 255)
                }

                val positioningManager = PositioningManager.getInstance()
                positioningManager.start(PositioningManager.LocationMethod.GPS_NETWORK)
            } else {
                Log.e(LOG_NAME, "Cannot initialize Map Fragment")
            }
        }

        btnFind.setOnClickListener {
            queryObjects(
                etObjectQuery.text.toString(),
                { _, routes ->
                    Log.d(LOG_NAME, "Objects query response with ${routes.size} routes")

                    runOnUiThread {
                        if (routes.isNotEmpty()) {
                            this.routes = arrayListOf(
                                IziTravelRoute(
                                    routes[0].uuid,
                                    routes[0].title,
                                    arrayListOf(
                                        GeoCoordinate(55.7180113, 37.559704),
                                        GeoCoordinate(55.7171799, 37.5613035)
                                    )
                                )
                            )
                        } else {
                            this.routes = routes
                        }
//                        this.routes = routes

                        routeTitles.clear()
                        routeTitles.addAll(routes.map { it.title })
                        routesAdapter.notifyDataSetChanged()

                        if (routes.isEmpty()) {
                            map?.removeAllMapObjects()
                        } else {
                            spinnerRoutes.setSelection(0)
                        }
                    }
                }
            )
        }

    }

    override fun onResume() {
        super.onResume()

        if (!setupLocationListener()) {
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                TAG_CODE_PERMISSION_LOCATION
            )
        }
    }

    override fun onPause() {
        super.onPause()
        locationManager.removeUpdates(locationListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        audioService.stopMedia()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == TAG_CODE_PERMISSION_LOCATION) {
            if (!setupLocationListener()) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    TAG_CODE_PERMISSION_LOCATION
                )
            }
        }
    }

    private fun setupLocationListener() : Boolean {
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000 * 10, 10f, locationListener
            )
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER, 1000 * 10, 10f,
                locationListener
            )
            return true
        } catch (e: SecurityException) {
            Log.e(LOG_NAME, "", e)
        }
        return false
    }

}
