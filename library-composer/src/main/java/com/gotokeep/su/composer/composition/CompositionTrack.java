package com.gotokeep.su.composer.composition;

import android.media.MediaFormat;
import android.net.Uri;
import android.support.annotation.Nullable;

import com.gotokeep.su.composer.source.MediaTrack;
import com.gotokeep.su.composer.time.Time;
import com.gotokeep.su.composer.time.TimeMapping;
import com.gotokeep.su.composer.time.TimeRange;

import java.util.ArrayList;
import java.util.List;

/**
 * A track in a composition object, consisting of a media type, a track identifier, and track segments.
 * if timeRange of two segments overlapped, the latter will be able to seen. Or it has a multi-input segment(such as
 * transition, effect) covered on them and result will be it.
 *
 * @author xana/cuixianming
 * @version 1.0
 * @since 2018/6/18 06:55
 */
public class CompositionTrack extends MediaTrack {

    private final List<CompositionSegment> segments = new ArrayList<>();
    private final List<CompositionInstruction> instructions = new ArrayList<>();

    public CompositionTrack(Uri source, int trackType, int trackIndex, String mimeType, TimeRange timeRange,
                            MediaFormat format) {
        super(source, trackType, trackIndex, mimeType, timeRange, format);
    }

    public CompositionTrack(@Nullable Uri source, @TrackType int trackType, int trackIndex, String mimeType,
                            Time duration, MediaFormat format) {
        super(source, trackType, trackIndex, mimeType, duration, format);
    }

    public CompositionTrack(@TrackType int trackType, int trackIndex) {
        super(null, trackType, trackIndex, null, Time.ZERO, null);
    }

    public CompositionTrack(@TrackType int trackType, int trackIndex, TimeRange timeRange) {
        super(null, trackType, trackIndex, null, timeRange, null);
    }

    public void addSegment(MediaTrack sourceTrack, TimeRange sourceTimeRange) {
        addSegment(sourceTrack, sourceTimeRange, sourceTimeRange.start);
    }

    public void addSegment(MediaTrack sourceTrack, TimeRange sourceTimeRange, Time atTime) {
        checkTrackType(sourceTrack);
        TimeRange targetRange = new TimeRange(Time.isInvalid(atTime) ? timeRange.getRangeEnd() : atTime,
                sourceTimeRange.duration);
        addSegment(new CompositionSegment(sourceTrack, new TimeMapping(sourceTimeRange, targetRange)));
    }

    public void addSegment(MediaTrack sourceTrack, TimeRange sourceTimeRange, float scale, Time atTime) {
        checkTrackType(sourceTrack);
        TimeRange targetTimeRange = new TimeRange(Time.isInvalid(atTime) ? timeRange.getRangeEnd() : atTime,
                sourceTimeRange.duration.multiply(scale));
        addSegment(new CompositionSegment(sourceTrack, new TimeMapping(sourceTimeRange, targetTimeRange)));
    }

    public void addSegment(CompositionSegment segment) {
        checkTrackType(segment.getSourceTrack());
        Time rangeEnd = timeRange.getRangeEnd();
        Time duration = segment.getTimeMapping().target.getRangeEnd();
        if (rangeEnd.compareTo(duration) < 0) {
            timeRange.duration = duration;
        }
        segments.add(segment);
    }

    public void addInstruction(CompositionInstruction instruction) {
        Time rangeEnd = timeRange.getRangeEnd();
        Time duration = instruction.getTimeRange().getRangeEnd();
        if (rangeEnd.compareTo(duration) < 0) {
            timeRange.duration = duration;
        }
        instructions.add(instruction);
    }

    private void checkTrackType(MediaTrack sourceTrack) {
        if (sourceTrack != null && sourceTrack.getTrackType() != trackType) {
            throw new IllegalArgumentException("source track does not match CompositionTrack type: " + sourceTrack
                    .getTrackType());
        }
    }

    public int getTrackType() {
        return trackType;
    }

    public TimeRange getTimeRange() {
        return timeRange;
    }

    public Time getSamplePresentationTimeForTrackTime(CompositionSegment segment, Time trackTime) {
        if (segment != null) {
            return segment.timeMapping.mapToSource(trackTime);
        } else {
            return Time.INVALID_TIME;
        }
    }
}
