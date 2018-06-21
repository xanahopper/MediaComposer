package com.gotokeep.su.composer.time;

import android.support.annotation.NonNull;

/**
 * Universal concept of time in MediaComposer
 *
 * @author xana/cuixianming
 * @version 1.0
 * @since 2018/6/18 09:59
 */
public class Time implements Comparable<Time> {
    public static final int FLAGS_VALID = 1;
    public static final int FLAGS_ROUNDED = 2;
    public static final int FLAGS_POSITIVE_INFINITY = 4;
    public static final int FLAGS_NEGATIVE_INFINITY = 8;
    public static final int FLAGS_INDEFINITE = 16;
    public static final int FLAGS_MASK = FLAGS_VALID | FLAGS_ROUNDED | FLAGS_POSITIVE_INFINITY |
            FLAGS_NEGATIVE_INFINITY | FLAGS_INDEFINITE;

    public static final Time INVALID_TIME = new Time(0, 0, 0);
    public static final Time ZERO = new Time(0);

    public long value;
    public int flags;
    /**
     * epoch can be consider like a repeat count which to distinguish from same timestamp in different loop.
     * nonzero epoch cannot be added or subtracted and 0 epoch is considered as a duration.
     */
    public int epoch;

    public Time(long value) {
        this(value, FLAGS_VALID, 0);
    }

    public Time(long value, int flags, int epoch) {
        this.value = value;
        this.flags = flags;
        this.epoch = epoch;
    }

    public static boolean isValid(Time time) {
        return time != null && (time.flags & FLAGS_VALID) != 0;
    }

    public static boolean isInvalid(Time time) {
        return !isValid(time);
    }

    public static boolean isPositiveInfinity(Time time) {
        return time != null && (time.flags & FLAGS_POSITIVE_INFINITY) != 0;
    }

    public static boolean isNegativeInfinity(Time time) {
        return time != null && (time.flags & FLAGS_NEGATIVE_INFINITY) != 0;
    }

    public static boolean isIndefinite(Time time) {
        return time != null && (time.flags & FLAGS_INDEFINITE) != 0;
    }

    public static boolean isNumberic(Time time) {
        return time != null && (time.flags & (FLAGS_VALID | FLAGS_MASK)) == FLAGS_VALID;
    }

    public static boolean isRounded(Time time) {
        return time != null && (time.flags & FLAGS_ROUNDED) != 0;
    }

    private <T extends Comparable<T>> int compare(T x, T y) {
        return x.compareTo(y);
    }

    @Override
    public int compareTo(@NonNull Time o) {
        if (isValid(this) && isValid(o)) {
            if (isNumberic(this) && isNumberic(o)) {
                return epoch == o.epoch ? compare(value, o.value) : compare(epoch, o.epoch);
            } else {
                return compare(flags & FLAGS_POSITIVE_INFINITY, o.flags & FLAGS_POSITIVE_INFINITY);
            }
        } else {
            return compare(flags & FLAGS_VALID, o.flags & FLAGS_VALID);
        }
    }

    public Time add(Time o) {
        if (isValid(this) && isValid(o)) {
            if (o.epoch == 0) {
                return new Time(value + o.value, flags, epoch);
            } else if (o.epoch == epoch) {
                return new Time(value + o.value, flags, 0);
            } else {
                return INVALID_TIME;
            }
        } else {
            return INVALID_TIME;
        }
    }

    public Time subtract(Time o) {
        if (isValid(this) && isValid(o)) {
            if (o.epoch == 0) {
                return new Time(value - o.value, flags, epoch);
            } else if (o.epoch == epoch) {
                return new Time(value - o.value, flags, 0);
            } else {
                return INVALID_TIME;
            }
        } else {
            return INVALID_TIME;
        }
    }

    public Time multiply(float multiplier) {
        if (isNumberic(this)) {
            return new Time((long) (value * multiplier), flags, epoch);
        } else {
            return INVALID_TIME;
        }
    }
}
