package com.gotokeep.su.composer.composition;

import com.gotokeep.su.composer.time.TimeRange;

import java.util.ArrayList;
import java.util.List;

/**
 * Project:  MediaComposer
 * Author:   Xana Hopper(xanahopper@163.com)
 * Created:  2018/6/24 23:18
 */
public abstract class CompositionInstruction {
    protected List<CompositionSegment> dependencies = new ArrayList<>();
    protected TimeRange timeRange;

    public TimeRange getTimeRange() {
        return timeRange;
    }

    public void setTimeRange(TimeRange timeRange) {
        this.timeRange = timeRange;
    }

    public int getDependenciesCount() {
        return dependencies.size();
    }

    public List<CompositionSegment> getDependencies() {
        return dependencies;
    }
}
