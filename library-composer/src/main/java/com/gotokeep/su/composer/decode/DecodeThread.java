package com.gotokeep.su.composer.decode;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.Nullable;
import android.view.Surface;

import com.gotokeep.su.composer.source.MediaTrack;

/**
 * @author xana/cuixianming
 * @version 1.0
 * @since 2018/6/21 16:41
 */
public final class DecodeThread implements Handler.Callback {
    public static final int STATE_IDLE = 0;
    public static final int STATE_CONFIGURED = 1;
    public static final int STATE_BUZY = 2;

    private Decoder decoder = new Decoder();
    private Surface decodeSurface;
    private boolean needReconfigure = false;

    private HandlerThread internalThread;
    private Handler handler;

    private int state;

    public DecodeThread(String type, int index) {
        internalThread = new HandlerThread("DecodeThread-" + type + "-" + index);
        internalThread.start();
        handler = new Handler(internalThread.getLooper(), this);
    }

    public void flush() {
        if (decoder.getState() == Decoder.STATE_STARTED) {
            decoder.getDecoder().flush();
        }
    }

    public void reconfigure(MediaTrack mediaTrack, @Nullable Surface decodeSurface) {
        this.decodeSurface = decodeSurface;
        if (!decoder.changeSource(mediaTrack, decodeSurface)) {
            // callback error
        }
        state = STATE_CONFIGURED;
    }

    public void stop() {

    }

    public void sendRequest(DecodeRequest request) {

    }

    public void release() {
        if (decoder.getState() != Decoder.STATE_UNINITIALIZED) {
            decoder.release();
        }
    }

    public Surface getDecodeSurface() {
        return decodeSurface;
    }

    public int getState() {
        return state;
    }

    @Override
    public boolean handleMessage(Message msg) {
        return false;
    }
}
