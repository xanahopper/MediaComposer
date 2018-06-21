package com.gotokeep.su.composer.render;

import android.support.annotation.CallSuper;

import com.gotokeep.su.composer.gles.ProgramObject;
import com.gotokeep.su.composer.gles.RenderTexture;
import com.gotokeep.su.composer.time.Time;

/**
 * @author xana/cuixianming
 * @version 1.0
 * @since 2018/6/21 12:03
 */
public abstract class VideoRenderNode implements VideoRenderTarget {
    protected RenderTexture targetTexture;
    protected boolean targetFrameAvailable = false;
    protected ProgramObject renderProgram;

    public VideoRenderNode() {
        renderProgram = createRenderProgram();
    }

    public RenderTexture getTargetTexture() {
        return targetTexture;
    }

    @Override
    public void setCurrentTarget() {
        if (targetTexture != null) {
            targetTexture.setRenderTarget();
        } else {
            RenderTexture.resetRenderTarget();
        }
    }

    @Override
    public boolean isTargetFrameAvailable() {
        return targetFrameAvailable;
    }

    public abstract void render(Time renderTimeUs);

    protected abstract ProgramObject createRenderProgram();
}
