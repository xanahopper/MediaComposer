package com.gotokeep.su.composer.source;

import com.gotokeep.su.composer.time.TimeMapping;
import com.gotokeep.su.composer.time.TimeRange;

/**
 * @author xana/cuixianming
 * @version 1.0
 * @since 2018/6/18 22:08
 */
public class MediaTrackSegment {
    protected TimeMapping timeMapping;
    protected boolean empty;
    protected MediaTrack sourceTrack;

    public MediaTrackSegment(TimeMapping timeMapping, boolean empty) {
        this.timeMapping = timeMapping;
        this.empty = empty;
    }

    public TimeMapping getTimeMapping() {
        return timeMapping;
    }

    public boolean isEmpty() {
        return empty;
    }
    public MediaTrackSegment(MediaTrack sourceTrack, TimeRange sourceTimeRange, TimeRange targetTimeTimeRange) {
        this(sourceTrack, new TimeMapping(sourceTimeRange, targetTimeTimeRange));
    }

    public MediaTrackSegment(TimeRange timeRange) {
        this(null, new TimeMapping(timeRange, timeRange));
    }

    public MediaTrackSegment(MediaTrack sourceTrack, TimeMapping timeMapping) {
        this(sourceTrack, timeMapping, !TimeMapping.isValid(timeMapping) || sourceTrack == null);
    }

    public MediaTrackSegment(MediaTrack sourceTrack, TimeMapping timeMapping, boolean empty) {
        this(timeMapping, empty);
        this.sourceTrack = sourceTrack;
    }

    public MediaTrack getSourceTrack() {
        return sourceTrack;
    }

    public static MediaTrackSegment createEmpty(TimeRange timeRange) {
        return new MediaTrackSegment(null, new TimeMapping(timeRange, timeRange), true);
    }
}
