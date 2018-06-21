package com.gotokeep.su.composer;

import android.graphics.Bitmap;
import android.support.annotation.IntDef;

import com.gotokeep.su.composer.MediaComposer;
import com.gotokeep.su.composer.timeline.Timeline;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author xana/cuixianming
 * @version 1.0
 * @since 2018/6/17 16:10
 */
public interface TimelineImageGenerator {

    int GENERATE_SUCCEEDED = 0;
    int GENERATE_FAILED = 1;
    int GENERATE_CANCELED = 2;

    @IntDef({GENERATE_SUCCEEDED, GENERATE_FAILED, GENERATE_CANCELED})
    @Retention(RetentionPolicy.SOURCE)
    @interface GenerateResult {}

    interface ImageGeneratorCallback {
        void onImageGenerated(Frame generatedFrame);

        void onError(Exception exception);
    }

    class Frame {
        public Bitmap image;
        public long requestedTimeUs;
        public long actualTimeUs;
        @GenerateResult public int generateResult;
    }

    void setTimeline(Timeline timeline);

    Frame getImageAtTime(long requestedTimeUs);

    void generateImagesAsync(long[] requestTimesUs, ImageGeneratorCallback completeCallback);

    void cancelAllGeneration();
}
