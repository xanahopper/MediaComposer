package com.gotokeep.su.composer.timeline;

import android.net.Uri;

import com.gotokeep.su.composer.time.TimeRange;

import org.jetbrains.annotations.NotNull;

/**
 * @author xana/cuixianming
 * @version 1.0
 * @since 2018/6/18 07:27
 */
public final class SourceItem extends TimelineItem {
    public Uri uri;
    public int priority;

    public SourceItem(@NotNull TimeRange timeRange) {
        super(timeRange);
    }

    @Override
    public int getSourceCount() {
        return 1;
    }

    @NotNull
    @Override
    public Uri[] getSources() {
        return new Uri[] {uri};
    }
}
