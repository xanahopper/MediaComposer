package com.gotokeep.su.composer.timeline

import android.net.Uri
import com.gotokeep.su.composer.time.TimeRange

/**
 * @author xana/cuixianming
 * @version 1.0
 * @since 2018/6/18 07:31
 */
abstract class TimelineItem(var timeRange: TimeRange) {
    abstract val sourceCount: Int
    abstract val sources: Array<Uri>

    fun getSource(index: Int): Uri = sources[index]
}
