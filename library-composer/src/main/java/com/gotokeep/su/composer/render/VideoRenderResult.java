package com.gotokeep.su.composer.render;

import android.support.v4.util.Pools;

import com.gotokeep.su.composer.gles.RenderTexture;

/**
 * Project:  MediaComposer
 * Author:   Xana Hopper(xanahopper@163.com)
 * Created:  2018/6/24 22:16
 */
public final class VideoRenderResult {
    private static final Pools.Pool<VideoRenderResult> pool = new Pools.SynchronizedPool<>(16);

    public static VideoRenderResult obtain() {
        VideoRenderResult result = pool.acquire();
        if (result == null) {
            result = new VideoRenderResult();
        } else {
            result.recycled = false;
        }

        return result;
    }

    public RenderTexture outputTexture;
    public long presentationTimeUs;
    public int flags;
    public boolean recycled = false;

    public void recycle() {
        if (!recycled) {
            pool.release(this);
            this.recycled = true;
        }
    }
}
