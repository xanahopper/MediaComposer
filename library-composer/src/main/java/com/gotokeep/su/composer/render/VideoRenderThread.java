package com.gotokeep.su.composer.render;

import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

import com.gotokeep.su.composer.gles.EglCore;
import com.gotokeep.su.composer.gles.RenderTexture;
import com.gotokeep.su.composer.gles.RenderUniform;

/**
 * Project:  MediaComposer
 * Author:   Xana Hopper(xanahopper@163.com)
 * Created:  2018/6/23 14:09
 */
public final class VideoRenderThread implements Handler.Callback {
    private static final int MSG_INIT = 237;
    private static final int MSG_SET_TARGET_SURFACE = 100;
    private static final int MSG_DO_RENDER = 437;
    private static final String TAG = "VideoRenderThread";

    private HandlerThread internalThread;
    private Handler handler;

    private EglCore eglCore;
    private EGLSurface eglSurface;

    private Surface targetSurface;
    private boolean ready = false;

    public VideoRenderThread() {
        internalThread = new HandlerThread("VideoRenderThread");
        internalThread.start();
        handler = new Handler(internalThread.getLooper(), this);
        handler.sendEmptyMessage(MSG_INIT);
    }

    public void setTargetSurface(Surface surface) {
        handler.obtainMessage(MSG_SET_TARGET_SURFACE, surface).sendToTarget();
    }

    public boolean isReady() {
        return ready;
    }

    public void sendRequest(RenderRequest request) {
        handler.obtainMessage(MSG_DO_RENDER, request).sendToTarget();
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_INIT:
                init();
                break;
            case MSG_SET_TARGET_SURFACE:
                setTargetSurfaceInternal((Surface) msg.obj);
                break;
            case MSG_DO_RENDER:
                if (!ready) {
                    handler.sendMessageDelayed(msg, 50);
                }
                RenderRequest request = (RenderRequest) msg.obj;
                doRender(request);
                request.recycle();
                break;
        }
        return false;
    }

    private void init() {
        eglCore = new EglCore(null, EglCore.FLAG_TRY_GLES3 | EglCore.FLAG_RECORDABLE);
        eglSurface = eglCore.createOffscreenSurface(1, 1);
        eglCore.makeCurrent(eglSurface);
        ready = true;
    }

    private void setTargetSurfaceInternal(Surface surface) {
        EGLSurface originSurface = eglSurface;
        if (surface == null) {
            eglSurface = eglCore.createOffscreenSurface(1, 1);
        } else {
            eglSurface = eglCore.createWindowSurface(surface);
        }
        targetSurface = surface;
        eglCore.makeCurrent(eglSurface);
        if (originSurface != null) {
            eglCore.releaseSurface(originSurface);
        }
        ready = true;
    }

    private void doRender(RenderRequest request) {
        // TODO: Wrap gl20 and gl30 in same interface
        if (request == null || request.recycled) {
            Log.w(TAG, "cannot render request because it is invalid");
            return;
        }
        if (request.renderProgram != null) {
            if (request.renderData != null) {
                request.renderProgram.use(request.renderData);
            } else {
                request.renderProgram.use();
            }
            if (request.targetTexture != null) {
                request.targetTexture.setRenderTarget();
            } else {
                RenderTexture.resetRenderTarget();
                eglCore.setPresentationTime(eglSurface, request.presentationTimeUs);
            }
            if (request.renderSize != null) {
                GLES20.glViewport(0, 0, request.renderSize.getWidth(), request.renderSize.getHeight());
            }
            for (int i = 0; i < request.sourceTextures.length; i++) {
                RenderTexture source = request.sourceTextures[i];
                if (source != null) {
                    if (source.isFrameAvailable()) {
                        source.updateTexImage();
                    }
                    source.bind(i);
                }
            }
            for (RenderUniform uniform : request.renderUniforms) {
                request.renderProgram.setUniform(uniform);
            }
            if (request.renderData != null) {
                request.renderData.draw();
            }
            if (request.targetTexture == null) {
                eglCore.swapBuffers(eglSurface);
            }
        }
        request.recycle();
    }
}
