package com.gotokeep.su.composer.util;

import android.annotation.SuppressLint;

import java.util.List;

/**
 * @author xana/cuixianming
 * @version 1.0
 * @since 2018/5/13 13:39
 */
public final class TimeUtil {
    public static final int MILLISECOND = 1000;
    public static final int BILLION_US = 1000_000;

    public static long secToMs(int sec) {
        return sec * MILLISECOND;
    }

    public static long secToUs(int sec) {
        return sec * BILLION_US;
    }

    public static long msToUs(long timeMs) {
        return timeMs * MILLISECOND;
    }

    public static long usToMs(long timeUs) {
        return Math.round((float) timeUs / MILLISECOND);
    }

    public static long usToSec(long timeUs) {
        return Math.round((float) timeUs / BILLION_US);
    }

    public static long nsToUs(long timeNs) {
        return Math.round((float) timeNs / MILLISECOND);
    }

    public static boolean inRange(long value, long low, long high) {
        return value >= low && value <= high;
    }

    @SuppressLint("DefaultLocale")
    public static String usToString(long timeUs) {
        return String.format("%02d:%03d", timeUs / 1000000, (timeUs % 1000000) / 1000);
    }

    public static long usToNs(long timeUs) {
        return timeUs * 1000;
    }

    public static long findClosestKeyFrame(List<Long> keyFrames, long positionUs) {
        int start = 0;
        int end = keyFrames.size() - 1;
        while (start <= end) {
            int mid = (start + end) / 2;
            long value = keyFrames.get(mid);
            if (positionUs == value) {
                return value;
            } else if (positionUs < value) {
                end = mid - 1;
            } else {
                start = mid + 1;
            }
        }
        int index = Math.min(start, end);
        if (index < 0) index = 0;
        else if (index > keyFrames.size() - 1) index = keyFrames.size() - 1;
        return keyFrames.get(index);
    }
}
