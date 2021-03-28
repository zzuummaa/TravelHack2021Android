package ru.zuma.travelhack2021android

import android.media.AudioManager
import android.media.MediaPlayer
import android.util.Log

class AudioService {
    private var mediaPlayer: MediaPlayer? = null
    private var lastPlayedPoint: IziTravelPointFull? = null

    fun playMedia(point: IziTravelPointFull) {
        val url = iziTravelAudioURL(point.contentProviderUUID, point.audioUUID)
        Log.d(javaClass.simpleName, "Request audio $url")

        if (lastPlayedPoint == point) return
        lastPlayedPoint = point


        if (url == null) return

        stopMedia()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(url)
            setAudioStreamType(AudioManager.STREAM_MUSIC)
            setOnPreparedListener { start() }
            prepareAsync()
        }
    }

    fun stopMedia() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer!!.release()
                mediaPlayer = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}