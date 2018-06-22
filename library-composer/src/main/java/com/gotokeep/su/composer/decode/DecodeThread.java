package com.gotokeep.su.composer.decode;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.Nullable;
import android.view.Surface;

/**
 * @author xana/cuixianming
 * @version 1.0
 * @since 2018/6/21 16:41
 */
public final class DecodeThread implements Handler.Callback {
    private String mimeType;
    private MediaCodec decoder;
    private Surface outputSurface;
    private boolean needReconfigure = false;

    private HandlerThread internalThread;
    private Handler handler;

    public DecodeThread(int index) {
        internalThread = new HandlerThread("DecodeThread-" + index);
        internalThread.start();
        handler = new Handler(internalThread.getLooper(), this);
    }

    public void flush() {

    }

    public void reconfigure(MediaFormat decodeFormat, @Nullable Surface decodeSurface) {

    }

    public void stop() {

    }

    public void release() {

    }

    public Surface getOutputSurface() {
        return outputSurface;
    }

    @Override
    public boolean handleMessage(Message msg) {
        return false;
    }
}
