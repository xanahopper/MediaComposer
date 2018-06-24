package com.gotokeep.su.composer.render;

import android.os.Handler;

/**
 * @author xana/cuixianming
 * @version 1.0
 * @since 2018-06-22 18:06
 */
public final class RenderEngine {
    private VideoRenderThread videoRenderThread;

    public RenderEngine(Handler handler) {
        videoRenderThread = new VideoRenderThread();
    }

    public void sendRequest(RenderRequest request) {

    }
}
