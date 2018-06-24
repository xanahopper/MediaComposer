package com.gotokeep.su.composer.decode;

import android.media.MediaExtractor;
import android.os.SystemClock;
import android.support.v4.util.Pools;
import android.view.Surface;

import com.gotokeep.su.composer.composition.CompositionSegment;

/**
 * @author xana/cuixianming
 * @version 1.0
 * @since 2018-06-01 18:29
 */
public class DecodeRequest {
    private static Pools.Pool<DecodeRequest> pool = new Pools.SynchronizedPool<>(32);

    public CompositionSegment requestMediaTrack;
    public MediaExtractor requestExtractor;
    public Surface decodeSurface;
    public long requestTimeMs;
    public long requestDecodeTimeUs;
    public boolean recycled;

    public static DecodeRequest obtain(CompositionSegment requestMediaTrackSegment, MediaExtractor extractor,
                                       Surface decodeSurface, long requestDecodeTimeUs) {
        DecodeRequest request = pool.acquire();
        if (request == null) {
            request = new DecodeRequest();
        }
        request.requestTimeMs = SystemClock.elapsedRealtime();
        request.requestDecodeTimeUs = requestDecodeTimeUs;
        request.requestMediaTrack = requestMediaTrackSegment;
        request.requestExtractor = extractor;
        request.decodeSurface = decodeSurface;
        request.recycled = false;
        return request;
    }

    public void recycle() {
        requestTimeMs = 0;
        requestDecodeTimeUs = 0;
        recycled = true;
        pool.release(this);
    }
}
