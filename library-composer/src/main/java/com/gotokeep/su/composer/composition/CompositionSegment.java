package com.gotokeep.su.composer.composition;

import com.gotokeep.su.composer.source.MediaTrack;
import com.gotokeep.su.composer.time.TimeMapping;
import com.gotokeep.su.composer.time.TimeRange;

/**
 * @author xana/cuixianming
 * @version 1.0
 * @since 2018/6/18 22:08
 */
public class CompositionSegment {
    protected TimeMapping timeMapping;
    protected boolean empty;
    protected MediaTrack sourceTrack;

    public CompositionSegment(TimeMapping timeMapping, boolean empty) {
        this.timeMapping = timeMapping;
        this.empty = empty;
    }

    public TimeMapping getTimeMapping() {
        return timeMapping;
    }

    public boolean isEmpty() {
        return empty;
    }

    public CompositionSegment(MediaTrack sourceTrack, TimeRange sourceTimeRange, TimeRange targetTimeTimeRange) {
        this(sourceTrack, new TimeMapping(sourceTimeRange, targetTimeTimeRange));
    }

    public CompositionSegment(TimeRange timeRange) {
        this(null, new TimeMapping(timeRange, timeRange));
    }

    public CompositionSegment(MediaTrack sourceTrack, TimeMapping timeMapping) {
        this(sourceTrack, timeMapping, !TimeMapping.isValid(timeMapping) || sourceTrack == null);
    }

    public CompositionSegment(MediaTrack sourceTrack, TimeMapping timeMapping, boolean empty) {
        this(timeMapping, empty);
        this.sourceTrack = sourceTrack;
    }

    public MediaTrack getSourceTrack() {
        return sourceTrack;
    }

    public static CompositionSegment createEmpty(TimeRange timeRange) {
        return new CompositionSegment(null, new TimeMapping(timeRange, timeRange), true);
    }
}
