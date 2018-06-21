package com.gotokeep.su.composer.decode;

import android.os.SystemClock;
import android.support.v4.util.Pools;

import com.gotokeep.su.composer.source.MediaTrack;

/**
 * @author xana/cuixianming
 * @version 1.0
 * @since 2018-06-01 18:29
 */
public class DecodeRequest {
    private static Pools.Pool<DecodeRequest> pool = new Pools.SynchronizedPool<>(32);

    public MediaTrack requestMediaTrack;
    public long requestTimeMs;
    public long requestDecodeTimeUs;

    public static DecodeRequest obtain() {
        DecodeRequest request = pool.acquire();
        if (request == null) {
            request = new DecodeRequest();
        }
        request.requestTimeMs = SystemClock.elapsedRealtime();
        return request;
    }

    public static DecodeRequest obtain(long requestDecodeTimeUs) {
        DecodeRequest request = obtain();
        request.requestDecodeTimeUs = requestDecodeTimeUs;
        return request;
    }

    public static DecodeRequest obtain(MediaTrack requestMediaTrack, long requestDecodeTimeUs) {
        DecodeRequest request = obtain();
        request.requestDecodeTimeUs = requestDecodeTimeUs;
        request.requestMediaTrack = requestMediaTrack;
        return request;
    }

    public void recycle() {
        requestTimeMs = 0;
        requestDecodeTimeUs = 0;
        pool.release(this);
    }
}
