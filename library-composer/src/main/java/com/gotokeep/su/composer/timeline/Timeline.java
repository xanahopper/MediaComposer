package com.gotokeep.su.composer.timeline;

import com.gotokeep.su.composer.time.TimeRange;

/**
 * @author xana/cuixianming
 * @version 1.0
 * @since 2018/6/16 18:51
 */
public interface Timeline {
    TimeRange getTimeRange();

    void addTrack(TimelineTrack track);

    TimelineTrack getSourceTrack();

    TimelineTrack getAudioTrack();

    int getTrackCount();

    TimelineTrack getTrack(int index);

    TimelineTrack[] getTracksAtTime(long timeMs);
}
