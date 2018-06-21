package com.gotokeep.su.composer.source;

import android.media.MediaFormat;
import android.net.Uri;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import com.gotokeep.su.composer.time.Time;
import com.gotokeep.su.composer.time.TimeMapping;
import com.gotokeep.su.composer.time.TimeRange;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

/**
 * @author xana/cuixianming
 * @version 1.0
 * @since 2018/6/17 17:05
 */
public class MediaTrack {
    public static final int TYPE_INVALID = -1;
    public static final int TYPE_VISUAL = 0;
    public static final int TYPE_AUDIBLE = 1;
    public static final int TYPE_METADATA = 3;

    @IntDef({TYPE_INVALID, TYPE_VISUAL, TYPE_AUDIBLE, TYPE_METADATA})
    @Retention(RetentionPolicy.SOURCE)
    public @interface TrackType {
    }

    protected Uri source;
    @TrackType
    protected int trackType;
    protected int trackIndex;
    protected String mimeType;
    protected MediaFormat format;
    protected TimeRange timeRange;
    protected List<MediaTrackSegment> segments = new ArrayList<>();

    public MediaTrack(Uri source, @TrackType int trackType, int trackIndex, String mimeType, Time duration,
                      MediaFormat format) {
        this.source = source;
        this.trackType = trackType;
        this.trackIndex = trackIndex;
        this.mimeType = mimeType;
        this.timeRange = new TimeRange(Time.ZERO, duration);
        this.format = format;
    }

    public MediaTrack(Uri source, @TrackType int trackType, int trackIndex, String mimeType, TimeRange timeRange,
                      MediaFormat format) {
        this.source = source;
        this.trackType = trackType;
        this.trackIndex = trackIndex;
        this.mimeType = mimeType;
        this.timeRange = timeRange;
        this.format = format;
    }

    @SuppressWarnings("unchecked")
    public void generateSegments() {
        segments.add(new MediaTrackSegment(new TimeMapping(timeRange, timeRange), false));
    }

    public Uri getSource() {
        return source;
    }

    public int getTrackIndex() {
        return trackIndex;
    }

    public String getMimeType() {
        return mimeType;
    }

    public TimeRange getTimeRange() {
        return timeRange;
    }

    public int getTrackType() {
        return trackType;
    }

    public List<MediaTrackSegment> getSegments() {
        return segments;
    }

    @SuppressWarnings("unchecked")
    public MediaTrackSegment[] getSegmentAtTime(Time time) {
        List<MediaTrackSegment> result = new ArrayList<>();
        for (MediaTrackSegment segment : segments) {
            if (segment.timeMapping.target.isTimeInRange(time)) {
                result.add(segment);
            }
        }
        return (MediaTrackSegment[]) result.toArray();
    }

    public Time getSamplePresentationTimeForTrackTime(MediaTrackSegment segment, Time trackTime) {
        if (segment != null) {
            return segment.timeMapping.mapToSource(trackTime);
        } else {
            return Time.INVALID_TIME;
        }
    }
}
