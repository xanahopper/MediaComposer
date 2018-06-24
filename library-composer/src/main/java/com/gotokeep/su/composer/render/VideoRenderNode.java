package com.gotokeep.su.composer.render;

import android.view.Surface;

import com.gotokeep.su.composer.gles.ProgramObject;
import com.gotokeep.su.composer.gles.RenderTexture;
import com.gotokeep.su.composer.time.Time;

/**
 * @author xana/cuixianming
 * @version 1.0
 * @since 2018/6/21 12:03
 */
public abstract class VideoRenderNode {
    protected RenderTexture targetTexture;
    protected boolean targetFrameAvailable = false;
    protected ProgramObject renderProgram;

    public VideoRenderNode() {
        renderProgram = createRenderProgram();
    }

    public RenderTexture getTargetRenderTexture() {
        return targetTexture;
    }

    public void makeCurrentTarget() {
        if (targetTexture != null) {
            targetTexture.setRenderTarget();
        } else {
            RenderTexture.resetRenderTarget();
        }
    }

    public boolean isTargetFrameAvailable() {
        return targetFrameAvailable;
    }

    public abstract void render(Time renderTimeUs);

    protected abstract ProgramObject createRenderProgram();
}
