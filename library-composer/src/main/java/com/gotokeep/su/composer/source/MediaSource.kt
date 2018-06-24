package com.gotokeep.su.composer.source

import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import com.gotokeep.su.composer.time.Time

import java.io.IOException
import java.util.ArrayList

/**
 * @author xana/cuixianming
 * @version 1.0
 * @since 2018/6/17 20:47
 */
internal class MediaSource(private val filePath: String) {
    private var available: Boolean = false
    private val tracks = ArrayList<MediaTrack>()

    init {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(filePath)
            loadTracks(extractor)
            available = true
        } catch (e: IOException) {
            available = false
        } finally {
            extractor.release()
        }
    }

    private fun loadTracks(extractor: MediaExtractor) {
        for (i in 0..extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            var trackType = MediaTrack.TYPE_INVALID
            if (mime != null) {
                trackType = when {
                    mime.startsWith("video/") -> MediaTrack.TYPE_VISUAL
                    mime.startsWith("audio/") -> MediaTrack.TYPE_AUDIBLE
                    mime.startsWith("text/") -> MediaTrack.TYPE_METADATA
                    else -> MediaTrack.TYPE_INVALID
                }
            }
            if (trackType >= 0) {
                var duration: Time = Time.INVALID_TIME
                if (format.containsKey(MediaFormat.KEY_DURATION)) {
                    duration = Time(format.getLong(MediaFormat.KEY_DURATION))
                }
                val track = MediaTrack(Uri.parse(filePath), trackType, i, mime, duration, format)
                tracks.add(track)
            }
        }
    }

    fun getTrackCount(): Int {
        return if (available) tracks.size else 0
    }

    fun getTrack(index: Int): MediaTrack? {
        return if (available) tracks[index] else null
    }

    fun getTrackWithTrackId(trackId: Int): MediaTrack? {
        return if (available) tracks.find { it.trackIndex == trackId } else null
    }

    fun getTracksWithType(trackType: Int): Array<MediaTrack> {
        return if (available) tracks.filter { it.trackType == trackType }.toTypedArray() else arrayOf()
    }

    fun isAvailable(): Boolean {
        return available
    }
}
