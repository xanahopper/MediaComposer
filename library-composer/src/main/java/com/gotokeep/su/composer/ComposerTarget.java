package com.gotokeep.su.composer;

import android.view.Surface;

import com.gotokeep.su.composer.render.VideoRenderTarget;

/**
 * @author xana/cuixianming
 * @version 1.0
 * @since 2018/6/21 12:09
 */
public interface ComposerTarget extends VideoRenderTarget {
    interface RenderTargetAvailableListener {
        void onTargetSurfaceAvailable(Surface targetSurface);
    }

    void setAvailableListener(RenderTargetAvailableListener listener);

    void release();
}
