package com.gotokeep.su.composer.composition

import com.gotokeep.su.composer.source.MediaTrack
import java.util.*

/**
 * @author xana/cuixianming
 * @version 1.0
 * @since 2018/6/18 06:49
 */
class Composition {

    private val tracks = ArrayList<CompositionTrack>()

    fun addTrack(@MediaTrack.TrackType trackType: Int): CompositionTrack {
        val compositionTrack = CompositionTrack(trackType, tracks.size)
        tracks.add(compositionTrack)
        return compositionTrack
    }

    fun getTrackCount(): Int {
        return tracks.size
    }

    fun getTracksWithType(trackType: Int): Array<CompositionTrack> {
        return tracks.filter { it.trackType == trackType }.toTypedArray()
    }

    fun getTrack(index: Int): CompositionTrack = tracks[index]

    fun getTrackWithTrackId(trackId: Int): CompositionTrack = tracks.first { it.trackIndex == trackId }

    fun isAvailable(): Boolean = true
}
