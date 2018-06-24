package com.gotokeep.su.composer.composition.transition;

import com.gotokeep.su.composer.composition.CompositionInstruction;
import com.gotokeep.su.composer.composition.CompositionSegment;
import com.gotokeep.su.composer.time.Time;
import com.gotokeep.su.composer.time.TimeRange;

/**
 * Project:  MediaComposer
 * @author xana/cuixianming
 * @since 2018/6/25 00:05
 */
public class CompositionTransition extends CompositionInstruction {
    public CompositionTransition(CompositionSegment startSegment, CompositionSegment endSegment, Time duration) {
        if (startSegment == null || endSegment == null) {
            return;
        }
        if (Time.isValid(duration)) {
            dependencies.add(startSegment);
            dependencies.add(endSegment);

            Time start;
            TimeRange firstRange = startSegment.getTimeMapping().target;
            TimeRange secondRange = endSegment.getTimeMapping().target;

            start = firstRange.getRangeEnd();
            timeRange = new TimeRange(start, duration);
//            if (firstRange.getRangeEnd().compareTo(secondRange.start) == 0) {
//                start = startSegment.getTimeMapping().target.getRangeEnd();
//                firstRange.duration.add()
//            } else if ()
        }
    }
}
