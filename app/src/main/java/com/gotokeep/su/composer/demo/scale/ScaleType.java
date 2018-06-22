package com.gotokeep.su.composer.demo.scale;

import android.support.annotation.NonNull;

/**
 * @author xana/cuixianming
 * @version 1.0
 * @since 2018-02-11 19:36
 */

public enum ScaleType {
    CENTER,
    CENTER_CROP,
    CENTER_INSIDE,
    FIT_CENTER,
    FIT_XY,
    MATRIX,
    NONE;

    @NonNull
    public static ScaleType fromOrdinal(int ordinal) {
        if (ordinal < 0 || ordinal > NONE.ordinal()) {
            return ScaleType.NONE;
        }

        return ScaleType.values()[ordinal];
    }
}
