package com.gotokeep.su.composer;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.view.Surface;

import com.gotokeep.su.composer.composition.Composition;
import com.gotokeep.su.composer.decode.DecodeEngine;
import com.gotokeep.su.composer.decode.DecodeRequest;
import com.gotokeep.su.composer.render.AudioRenderNode;
import com.gotokeep.su.composer.render.RenderEngine;
import com.gotokeep.su.composer.source.MediaTrack;
import com.gotokeep.su.composer.time.Time;
import com.gotokeep.su.composer.util.Size;

/**
 * Project:  MediaComposer
 * Author:   Xana Hopper(xanahopper@163.com)
 * Created:  2018/6/24 12:28
 */
/* package-private */final class ComposerSynchronizer implements Handler.Callback {
    private static final int MSG_START = 257;
    private static final int MSG_STOP = 777;
    private static final int MSG_RENDER_VIDEO = 213;
    private static final int MSG_RENDER_AUDIO = 62;

    public static final int MSG_VIDEO_DECODED = 93;
    public static final int MSG_AUDIO_DECODED = 94;
    public static final int MSG_VIDEO_RENDERED = 970;
    public static final int MSG_AUDIO_RENDERED = 724;
    private final Context context;

    private DecodeEngine decodeEngine;
    private RenderEngine renderEngine;

    private HandlerThread internalThread;
    private Handler handler;
    private Handler eventHandler;
    private boolean started = false;

    public ComposerSynchronizer(Context context, Handler eventHandler) {
        this.context = context;
        this.eventHandler = eventHandler;

        decodeEngine = new DecodeEngine(handler);
        renderEngine = new RenderEngine(handler);

        internalThread = new HandlerThread("ComposerSync");
        internalThread.start();
        this.handler = new Handler(internalThread.getLooper(), this);
    }

    public void start() {
        handler.sendEmptyMessage(MSG_START);
    }

    public void stop() {
        handler.sendEmptyMessage(MSG_STOP);
    }

    public void decode(DecodeRequest request) {
        handler.obtainMessage(MSG_RENDER_VIDEO, request).sendToTarget();
    }

    public void render(AudioRenderNode node) {
        handler.obtainMessage(MSG_RENDER_AUDIO, node).sendToTarget();
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_START:
                started = true;
                break;
            case MSG_STOP:
                started = false;
                break;
            case MSG_RENDER_VIDEO:
                DecodeRequest videoRequest = (DecodeRequest) msg.obj;
                decodeEngine.sendVideoRequest(videoRequest);
                break;
            case MSG_RENDER_AUDIO:
                DecodeRequest audioRequest = (DecodeRequest) msg.obj;
                decodeEngine.sendAudioRequest(audioRequest);
            case MSG_VIDEO_DECODED:

        }
        return false;
    }

    public void flush() {

    }

    public void setExportMode(boolean export) {

    }

    // feed items to decoder to until get decoded frame and wait for starting and rendering
    public void prepareVideo(MediaTrack[] sourceTrackItems) {

    }

    public void prepareAudio(MediaTrack[] sourceTrackItems) {

    }

    public void seekTo(Time seekTime) {

    }

    public void configure(Surface targetSurface, Size targetSize) {

    }

    public void setComposition(Composition composition) {

    }
}
