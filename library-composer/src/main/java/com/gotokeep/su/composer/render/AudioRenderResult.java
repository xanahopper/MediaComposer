package com.gotokeep.su.composer.render;

import android.support.v4.util.Pools;

/**
 * Project:  MediaComposer
 * Author:   Xana Hopper(xanahopper@163.com)
 * Created:  2018/6/24 21:56
 */
public final class AudioRenderResult {
    private static Pools.Pool<AudioRenderResult> pool = new Pools.SynchronizedPool<>(32);

    public static AudioRenderResult obtain() {
        AudioRenderResult result = pool.acquire();
        if (result == null) {
            result = new AudioRenderResult();
        } else {
            result.recycled = false;
        }
        return result;
    }

    public byte chunk[];
    public int bufferSize;
    public long presentationTimeUs;
    public int flags;
    public boolean recycled = false;

    public void recycle() {
        if (!recycled) {
            pool.release(this);
            recycled = true;
        }
    }
}
