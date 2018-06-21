package com.gotokeep.su.composer.time;

/**
 * @author xana/cuixianming
 * @version 1.0
 * @since 2018/6/18 06:59
 */
public class TimeMapping {
    public TimeRange source;
    public TimeRange target;

    public TimeMapping(TimeRange source, TimeRange target) {
        this.source = source;
        this.target = target;
    }

    public Time mapToTarget(Time sourceTime) {
        if (TimeRange.isValid(source) && Time.isValid(sourceTime)) {
            long value = (long) ((float) (sourceTime.value - source.start.value) / source.duration.value *
                                target.duration.value + target.start.value);
            return new Time(value, sourceTime.flags, sourceTime.epoch);
        } else {
            return sourceTime;
        }
    }

    public Time mapToSource(Time targetTime) {
        if (TimeRange.isValid(source) && Time.isValid(targetTime)) {
            long value = (long)((float)(targetTime.value - target.start.value) / target.duration.value *
                    source.duration.value + source.start.value);
            return new Time(value, targetTime.flags, targetTime.epoch);
        } else {
            return targetTime;
        }
    }

    public static boolean isValid(TimeMapping mapping) {
        return mapping != null && TimeRange.isValid(mapping.target);
    }
}
