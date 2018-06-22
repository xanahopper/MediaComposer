package com.gotokeep.su.composer.timeline

import android.support.annotation.IntDef
import com.gotokeep.su.composer.time.Time
import com.gotokeep.su.composer.time.TimeRange

/**
 * @author xana/cuixianming
 * @version 1.0
 * @since 2018/6/18 07:30
 */
class TimelineTrack<T : TimelineItem>(@field:TrackType
                                      private val trackType: Int) {

    private val items = mutableListOf<T>()
    private val timeRange = TimeRange(Time.ZERO, Time.ZERO)

    @IntDef(TRACK_TYPE_SOURCE, TRACK_TYPE_TRANSITION, TRACK_TYPE_FILTER, TRACK_TYPE_LAYER)
    @Retention(AnnotationRetention.SOURCE)
    internal annotation class TrackType

    fun clear() {
        items.clear()
    }

    fun addItem(item: T) {
        items.add(item)
        updateTimeRange()
    }

    fun removeItem(item: T) {
        items.remove(item)
        updateTimeRange()
    }

    fun getItemsAtTime(timeMs: Time): Array<TimelineItem> {
        return items.filter { it.timeRange.isTimeInRange(timeMs) }.toTypedArray()
    }

    private fun updateTimeRange() {
        var minStart = Time.INVALID_TIME
        var maxEnd = Time.ZERO
        for (item in items) {
            if (minStart < item.timeRange.start) {
                minStart = item.timeRange.start
            }
            if (maxEnd < item.timeRange.rangeEnd) {
                maxEnd = item.timeRange.rangeEnd
            }
        }
        if (minStart != Time.INVALID_TIME) {
            timeRange.start = minStart
        }
        if (maxEnd >= minStart) {
            timeRange.duration = maxEnd.subtract(minStart)
        }
    }

    companion object {
        const val TRACK_TYPE_SOURCE = 0
        const val TRACK_TYPE_TRANSITION = 1
        const val TRACK_TYPE_FILTER = 2
        const val TRACK_TYPE_LAYER = 3
    }
}
