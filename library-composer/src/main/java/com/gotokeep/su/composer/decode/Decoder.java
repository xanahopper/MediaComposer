package com.gotokeep.su.composer.decode;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.view.Surface;

import com.gotokeep.su.composer.source.MediaTrack;

import java.io.IOException;

/**
 * Project:  MediaComposer
 * Author:   Xana Hopper(xanahopper@163.com)
 * Created:  2018/6/23 12:25
 */
public final class Decoder {

    public static final int STATE_UNINITIALIZED = 0;
    public static final int STATE_INITIALIZED = 1;
    public static final int STATE_CONFIGURED = 2;
    public static final int STATE_STARTED = 3;

    private MediaCodec decoder;
    private String mimeType;
    private MediaFormat inputFormat;
    private MediaFormat outputFormat;
    private long presentationTimeUs;
    private int state = STATE_UNINITIALIZED;
    public final Object decoderSyncObj = new Object();

    public boolean changeSource(MediaTrack track, Surface decodeSurface) {
        synchronized (decoderSyncObj) {
            if (state == STATE_UNINITIALIZED) {
                setupDecoder(track);
            }
            if (mimeType == null || !mimeType.equals(track.getMimeType())) {
                if (state != STATE_UNINITIALIZED) {
                    release();
                }
                setupDecoder(track);
            } else {
                if (state == STATE_STARTED) {
                    decoder.stop();
                    state = STATE_INITIALIZED;
                }
            }
            if (state == STATE_INITIALIZED) {
                try {
                    decoder.configure(track.getFormat(), decodeSurface, null, 0);
                    state = STATE_CONFIGURED;
                    inputFormat = track.getFormat();
                    presentationTimeUs = 0;
                } catch (RuntimeException e) {
                    release();
                }
            }
            if (state == STATE_CONFIGURED) {
                decoder.start();
                state = STATE_STARTED;
            }
        }
        return state == STATE_STARTED;
    }

    public void setOutputFormat(MediaFormat mediaFormat) {
        outputFormat = mediaFormat;
    }

    public void release() {
        synchronized (decoderSyncObj) {
            decoder.release();
            decoder = null;
            state = STATE_UNINITIALIZED;
        }
    }

    public MediaCodec getDecoder() {
        return decoder;
    }

    public String getMimeType() {
        return mimeType;
    }

    public MediaFormat getInputFormat() {
        return inputFormat;
    }

    public MediaFormat getOutputFormat() {
        return outputFormat;
    }

    public long getPresentationTimeUs() {
        return presentationTimeUs;
    }

    public int getState() {
        return state;
    }

    /**
     * make STATE_INITIALIZED
     * @param track source track
     */
    private void setupDecoder(MediaTrack track) {
        try {
            decoder = MediaCodec.createDecoderByType(track.getMimeType());
            state = STATE_INITIALIZED;
            mimeType = track.getMimeType();
        } catch (IOException e) {
            decoder = null;
            state = STATE_UNINITIALIZED;
            mimeType = null;
        }
    }
}
