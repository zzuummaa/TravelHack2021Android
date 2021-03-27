package ru.zuma.travelhack2021android

import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.FragmentActivity
import com.here.android.mpa.common.GeoCoordinate
import com.here.android.mpa.common.MapSettings.setDiskCacheRootPath
import com.here.android.mpa.common.OnEngineInitListener
import com.here.android.mpa.mapping.AndroidXMapFragment
import com.here.android.mpa.mapping.Map
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File


class MainActivity : FragmentActivity() {
    private val LOG_NAME = javaClass.simpleName

    private var routeTitles = ArrayList<String>()
    private var routes = ArrayList<IziTravelRoute>()
    private var pointsFull = ArrayList<IziTravelPointFull>()

    private var mediaPlayer: MediaPlayer? = null

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
                map.removeAllMapObjects()
                calculateDisplayRoute(map, routes[position])

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
            val point = pointsFull[0]
            val url = iziTravelAudioURL(point.contentProviderUUID, point.audioUUID)
            Log.d(LOG_NAME, "Request audio $url")

            releaseMP()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(url)
                setAudioStreamType(AudioManager.STREAM_MUSIC)
                setOnPreparedListener { start() }
                prepareAsync()
            }

        }

        btnStopAudio.setOnClickListener {
            releaseMP()
        }

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
                    GeoCoordinate(55.7522200, 37.6155600, 0.0),
                    Map.Animation.NONE
                )
                // Set the zoom level to the average between min and max
                map.setZoomLevel((map.getMaxZoomLevel() + map.getMinZoomLevel()) / 2)
            } else {
                Log.e(LOG_NAME, "Cannot initialize Map Fragment")
            }
        })

        btnFind.setOnClickListener {
            queryObjects(
                etObjectQuery.text.toString(),
                { _, routes ->
                    Log.d(LOG_NAME, "Objects query response with ${routes.size} routes")

                    runOnUiThread {
                        this.routes = routes
                        routeTitles.clear()
                        routeTitles.addAll(routes.map { it.title })
                        routesAdapter.notifyDataSetChanged()

                        if (routes.isEmpty()) {
                            map.removeAllMapObjects()
                        } else {
                            spinnerRoutes.setSelection(0)
                        }
                    }
                }
            )
        }
    }

    private fun releaseMP() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer!!.release()
                mediaPlayer = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseMP()
    }
}
