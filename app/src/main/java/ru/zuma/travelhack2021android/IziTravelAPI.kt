package ru.zuma.travelhack2021android

import android.util.Log
import com.here.android.mpa.common.GeoCoordinate
import okhttp3.*
import org.json.JSONArray
import java.io.IOException

private val client = OkHttpClient()

private const val PACKAGE_NAME = "IziTravel"

class IziTravelRoute(
    val title: String,
    val points: List<GeoCoordinate>
)

fun queryObjects(query: String,
                 onSuccess: (call: Call, routes: ArrayList<IziTravelRoute>) -> Unit = { _, _ ->  },
                 onFailure: (call: Call, e: IOException) -> Unit = { _, e -> Log.e(PACKAGE_NAME, "", e) }) {

    val request = Request.Builder()
        .url("https://api.izi.travel/mtg/objects/search?languages=ru,en&includes=all&api_key=7c6c2db9-d237-4411-aa0e-f89125312494&query=${query}")
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            onFailure(call, e)
        }

        override fun onResponse(call: Call, response: Response) {
            response.use {
                if (!response.isSuccessful) Log.e(PACKAGE_NAME, "Unexpected code $response")
                val jsonArray = JSONArray(response.body!!.string())

                val routes = ArrayList<IziTravelRoute>()
                for (i in 0 until jsonArray.length()) {
                    val jsonRoute = jsonArray.getJSONObject(i)
                    if (!jsonRoute.has("route")) continue

                    if (jsonRoute.getString("category") != "walk") continue

                    val stringLatLonArray = jsonArray.getJSONObject(i).getString("route")
                    val points = stringLatLonArray.split(";")
                        .map { it ->
                            val latLong =  it.split(",").map { it -> it.toDouble() }
                            GeoCoordinate(latLong[0], latLong[1])
                        }

                    val title = jsonRoute.getString("title")

                    routes.add(IziTravelRoute(title, points))
                }

                onSuccess(call, routes)
            }
        }
    })
}