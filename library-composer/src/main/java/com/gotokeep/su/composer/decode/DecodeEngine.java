package com.gotokeep.su.composer.decode;

import android.view.Surface;

import java.nio.ByteBuffer;
import java.util.concurrent.Semaphore;

/**
 * @author xana/cuixianming
 * @version 1.0
 * @since 2018-06-22 17:55
 */
public final class DecodeEngine {
    private static final int DECODE_THREAD_COUNT = 4;
    private DecodeThread threads[] = new DecodeThread[DECODE_THREAD_COUNT];

    public DecodeEngine() {
        for (int i = 0; i < DECODE_THREAD_COUNT; i++) {
            threads[i] = new DecodeThread(i);
        }
    }

    public void sendVideoRequest(DecodeRequest request, Surface outputSurface) {

    }

    public Semaphore sendAudioRequest(DecodeRequest request, ByteBuffer outputBuffer) {
        return null;
    }
}
