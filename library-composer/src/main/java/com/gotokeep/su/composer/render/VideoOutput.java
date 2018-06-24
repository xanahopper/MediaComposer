package com.gotokeep.su.composer.render;

import android.view.Surface;

import com.gotokeep.su.composer.gles.RenderTexture;
import com.gotokeep.su.composer.util.Size;

/**
 * @author xana/cuixianming
 * @version 1.0
 * @since 2018/6/17 16:27
 */
public interface VideoOutput {

    Surface getTargetSurface();

    Size getTargetSize();

    RenderTexture getTargetRenderTexture();

    boolean isAvailable();

    void release();

    void renderFrame(RenderTexture renderTexture, long presentationTimeUs, int flags);

}
