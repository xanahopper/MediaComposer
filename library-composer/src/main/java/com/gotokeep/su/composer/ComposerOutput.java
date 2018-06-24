package com.gotokeep.su.composer;

import android.view.Surface;

import com.gotokeep.su.composer.render.AudioOutput;
import com.gotokeep.su.composer.render.VideoOutput;

/**
 * Composer render target with {@link Surface} to render video and method to write audio chunk
 *
 * @author xana/cuixianming
 * @version 1.0
 * @since 2018/6/21 12:09
 */
public interface ComposerOutput extends VideoOutput, AudioOutput {

    void setAvailableListener(OnAvailableListener listener);

    boolean isExport();

    interface OnAvailableListener {
        void onTargetAvailable(ComposerOutput target);

        void onTargetUnavailable();
    }
}
