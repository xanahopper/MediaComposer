package com.gotokeep.su.composer.decode;

import android.os.Handler;

import java.nio.ByteBuffer;

/**
 * @author xana/cuixianming
 * @version 1.0
 * @since 2018-06-22 17:55
 */
public final class DecodeEngine {
    private static final int VIDEO_DECODER_COUNT = 4;
    private static final int AUDIO_DECODER_COUNT = 2;
    private static final String VIDEO_TYPE = "video";
    private static final String AUDIO_TYPE = "audio";

    private DecodeThread videoThreads[] = new DecodeThread[VIDEO_DECODER_COUNT];
    private DecodeThread audioThreads[] = new DecodeThread[AUDIO_DECODER_COUNT];

    public DecodeEngine(Handler handler) {
        for (int i = 0; i < VIDEO_DECODER_COUNT; i++) {
            videoThreads[i] = new DecodeThread(VIDEO_TYPE, i);
        }
        for (int i = 0; i < AUDIO_DECODER_COUNT; i++) {
            audioThreads[i] = new DecodeThread(AUDIO_TYPE, VIDEO_DECODER_COUNT + i);
        }
    }

    public void cancelVideoRequest() {

    }

    public void sendVideoRequest(DecodeRequest request) {
        DecodeThread decodeThread = null;
        for (DecodeThread thread : videoThreads) {
            if (thread.getDecodeSurface() == request.decodeSurface) {
                decodeThread = thread;
                break;
            }
        }
        if (decodeThread == null) {
            for (DecodeThread thread : videoThreads) {
                if (thread.getState() == DecodeThread.STATE_IDLE) {
                    decodeThread = thread;
                    thread.reconfigure(request.requestMediaTrack.getSourceTrack(), request.decodeSurface);
                    break;
                }
            }
        }
        if (decodeThread != null) {
            decodeThread.sendRequest(request);
        }
    }

    public void sendAudioRequest(DecodeRequest request) {
        return;
    }
}
