package ru.zuma.travelhack2021android

import android.util.Log
import com.here.android.mpa.common.GeoCoordinate
import okhttp3.*
import org.json.JSONArray
import java.io.IOException

private val client = OkHttpClient()

private const val PACKAGE_NAME = "IziTravel"

class IziTravelRoute(
    val uuid: String,
    val title: String,
    val points: List<GeoCoordinate>
)

class IziTravelPoint(
    val uuid: String,
    val title: String
)

class IziTravelPointFull(
    val uuid: String,
    val contentProviderUUID: String? = null,
    val audioUUID: String? = null
) {
    override fun toString(): String {
        return "IziTravelPointFull(uuid='$uuid', contentProviderUUID='$contentProviderUUID', audioUUID='$audioUUID')"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IziTravelPointFull

        if (uuid != other.uuid) return false
        if (contentProviderUUID != other.contentProviderUUID) return false
        if (audioUUID != other.audioUUID) return false

        return true
    }
}

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
                    if (!jsonRoute.has("uuid")) continue

                    if (jsonRoute.getString("category") != "walk") continue

                    val stringLatLonArray = jsonArray.getJSONObject(i).getString("route")
                    val points = stringLatLonArray.split(";")
                        .map { it ->
                            val latLong =  it.split(",").map { it -> it.toDouble() }
                            GeoCoordinate(latLong[0], latLong[1])
                        }

                    val uuid = jsonRoute.getString("uuid")
                    val title = jsonRoute.getString("title")

                    routes.add(IziTravelRoute(uuid, title, points))
                }

                onSuccess(call, routes)
            }
        }
    })
}

typealias QuerySuccessCallback<T> = (call: Call, routes: T) -> Unit

fun queryObjectFull(objectUUID: String,
                    onSuccess: (call: Call, routes: ArrayList<IziTravelPoint>) -> Unit = { _, _ ->  },
                    onFailure: (call: Call, e: IOException) -> Unit = { _, e -> Log.e(PACKAGE_NAME, "", e) }) {
    val requestString = "https://api.izi.travel/mtgobjects/$objectUUID?languages=ru,en&includes=all&except=translations,publisher,download&api_key=7c6c2db9-d237-4411-aa0e-f89125312494";
    Log.d(PACKAGE_NAME, "Request: $requestString")
    val request = Request.Builder()
        .url(requestString)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            onFailure(call, e)
        }

        override fun onResponse(call: Call, response: Response) {
            response.use {
                if (!response.isSuccessful) Log.e(PACKAGE_NAME, "Unexpected code $response")
                val jsonObject = JSONArray(response.body!!.string()).getJSONObject(0)

                val jsonContent = jsonObject.getJSONArray("content")
                if (jsonContent.length() == 0) Log.e(PACKAGE_NAME, "Unexpected response: jsonContent.length() == 0")

                val jsonPoints = jsonContent.getJSONObject(0).getJSONArray("children")

                val points = ArrayList<IziTravelPoint>()
                for (i in 0 until jsonPoints.length()) {
                    val jsonPoint = jsonPoints.getJSONObject(i)
                    val title = jsonPoint.getString("title")
                    val uuid = jsonPoint.getString("uuid")
                    points.add(IziTravelPoint(uuid, title))
                }

                onSuccess(call, points)
            }
        }
    })
}

fun queryPointFull(pointUUID: String,
                   onSuccess: (call: Call, routes: IziTravelPointFull) -> Unit = { _, _ ->  },
                   onFailure: (call: Call, e: IOException) -> Unit = { _, e -> Log.e(PACKAGE_NAME, "", e) }) {

    val requestString = "https://api.izi.travel/mtgobjects/$pointUUID?languages=ru,en&includes=all&except=translations,publisher,download&api_key=7c6c2db9-d237-4411-aa0e-f89125312494";
    Log.d(PACKAGE_NAME, "Request: $requestString")
    val request = Request.Builder()
        .url(requestString)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            onFailure(call, e)
        }

        override fun onResponse(call: Call, response: Response) {
            response.use {
                if (!response.isSuccessful) Log.e(PACKAGE_NAME, "Unexpected code $response")
                val jsonObject = JSONArray(response.body!!.string()).getJSONObject(0)

                val uuid = jsonObject.getString("uuid")
                val contentProviderUUID = jsonObject.getJSONObject("content_provider").getString("uuid")

                val jsonContent = jsonObject.getJSONArray("content").getJSONObject(0)
                if (!jsonContent.has("audio")) {
                    // TODO log.e
                    return
                }
                val jsonAudioArray = jsonContent.getJSONArray("audio")

                if (jsonAudioArray.length() == 0) Log.e(PACKAGE_NAME, "Unexpected code $response")
                val audioUUID = jsonAudioArray.getJSONObject(0).getString("uuid")

                onSuccess(call, IziTravelPointFull(uuid, contentProviderUUID, audioUUID))
            }
        }
    })
}

fun iziTravelAudioURL(contentProviderUUID: String?, audioUUID: String?): String? {
    if (contentProviderUUID == null || audioUUID == null) return null
    return "https://media.izi.travel/$contentProviderUUID/$audioUUID.m4a?api_key=7c6c2db9-d237-4411-aa0e-f89125312494"
}