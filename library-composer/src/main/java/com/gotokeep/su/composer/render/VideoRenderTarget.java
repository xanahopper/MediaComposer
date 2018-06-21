package com.gotokeep.su.composer.render;

/**
 * @author xana/cuixianming
 * @version 1.0
 * @since 2018/6/17 16:27
 */
public interface VideoRenderTarget {
    void setCurrentTarget();

    boolean isTargetFrameAvailable();
}
