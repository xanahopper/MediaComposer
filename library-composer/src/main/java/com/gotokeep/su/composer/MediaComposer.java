package com.gotokeep.su.composer;

import com.gotokeep.su.composer.time.TimeRange;
import com.gotokeep.su.composer.timeline.Timeline;

/**
 * @author xana/cuixianming
 * @version 1.0
 * @since 2018/6/16 18:50
 */
public interface MediaComposer {
    void setTimeline(Timeline timeline);

    void setTarget(ComposerOutput target);

    void start();

    void stop();

    void seekTo(long seekTimeMs);

    void setLoop(boolean loop);

    void addEventListener(EventListener listener);

    void removeEventListener(EventListener listener);

    void release();

    interface EventListener {
        void onTimelineChanged(MediaComposer composer, Timeline timeline);

        void onPreprocess(MediaComposer composer/*, MediaItem item, long currentTimeMs, long totalTimeMs*/);

        void onStart(MediaComposer composer);

        void onPause(MediaComposer composer);

        void onStop(MediaComposer composer);

        void onPositionChange(MediaComposer composer, long currentTime, TimeRange totalTime);

        void onError(MediaComposer composer, Exception exception);
    }
}
