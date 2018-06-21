package com.gotokeep.su.composer.time;

/**
 * @author xana/cuixianming
 * @version 1.0
 * @since 2018/6/17 09:02
 */
public final class TimeRange {
    public Time start;
    public Time duration;

    public TimeRange(Time start, Time duration) {
        this.start = start;
        this.duration = duration;
    }

    public TimeRange(TimeRange src) {
        this(src.start, src.duration);
    }

    public Time getRangeEnd() {
        return start.add(duration);
    }

    public TimeRange scaleTimeRange(float scale) {
        if (isValid(this)) {
            return new TimeRange(start, duration.multiply(scale));
        } else {
            return this;
        }
    }

    public boolean isTimeInRange(Time time) {
        return time.compareTo(start) >= 0 && time.compareTo(getRangeEnd()) < 0;
    }

    public static boolean isValid(TimeRange timeRange) {
        return timeRange != null && Time.isValid(timeRange.start) && Time.isValid(timeRange.duration) &&
                timeRange.duration.epoch == 0 && timeRange.duration.value >= 0;
    }
}
