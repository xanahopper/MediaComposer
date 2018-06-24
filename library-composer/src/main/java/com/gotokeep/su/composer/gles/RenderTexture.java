package com.gotokeep.su.composer.gles;

import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.support.annotation.IntDef;
import android.util.Log;
import android.view.Surface;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * @author xana/cuixianming
 * @version 1.0
 * @since 2018/5/12 15:40
 */
public final class RenderTexture implements SurfaceTexture.OnFrameAvailableListener {
    private static final String TAG = RenderTexture.class.getSimpleName();

    public static final int TEXTURE_NATIVE = GLES20.GL_TEXTURE_2D;
    public static final int TEXTURE_EXTERNAL = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
    public static int CURRENT_TARGET_TEXTURE = 0;

    public boolean isFrameAvailable() {
        return frameAvailable;
    }

    public void updateTexImage() {
        surfaceTexture.updateTexImage();
        synchronized (frameSyncObj) {
            frameAvailable = false;
        }
    }

    @IntDef({TEXTURE_NATIVE, TEXTURE_EXTERNAL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface TextureTarget {

    }
    private int framebufferId = 0;

    private int textureTarget = 0;
    private int textureId = 0;
    private int textureWidth = 0;
    private int textureHeight = 0;
    private SurfaceTexture surfaceTexture;
    private Surface surface;
    private boolean released = false;
    private final Object frameSyncObj = new Object();
    private float transitionMatrix[] = new float[16];

    private boolean frameAvailable = false;

    public RenderTexture() {
        this.textureTarget = TEXTURE_EXTERNAL;
        this.surfaceTexture = null;
    }

    public RenderTexture(@TextureTarget int textureTarget, int width, int height) {
        this.textureWidth = width;
        this.textureHeight = height;
        createTexture(textureTarget);
    }

    public void setSize(int width, int height) {
        if (textureWidth == width && textureHeight == height) {
            return;
        }
        textureWidth = width;
        textureHeight = height;
        if (textureId > 0) {
            bind();
            GLES20.glTexImage2D(textureTarget, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA,
                    GLES20.GL_UNSIGNED_BYTE, null);
            unbind();
        }
    }

    public void clear() {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
    }

    private void createTexture(int textureTarget) {
        int texId[] = new int[1];
        GLES20.glGenTextures(1, texId, 0);
        this.textureTarget = textureTarget;
        this.textureId = texId[0];

        GLES20.glBindTexture(textureTarget, textureId);
        GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glBindTexture(textureTarget, 0);
        if (textureTarget == TEXTURE_EXTERNAL) {
            surfaceTexture = new SurfaceTexture(textureId);
            surfaceTexture.setOnFrameAvailableListener(this);
        } else {
            GLES20.glTexImage2D(textureTarget, 0, GLES20.GL_RGBA, textureWidth, textureHeight, 0,
                    GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        }
    }

    public void release() {
        if (surfaceTexture == null || released || textureId == 0) {
            return;
        }
        surfaceTexture.release();
        surfaceTexture = null;

        if (framebufferId > 0) {
            int ids[] = {framebufferId};
            GLES20.glDeleteFramebuffers(1, ids, 0);
            framebufferId = 0;
        }

        if (textureId > 0) {
            int ids[] = {textureId};
            GLES20.glDeleteTextures(1, ids, 0);
            textureId = 0;
        }

        released = true;
    }

    public boolean isReleased() {
        return released;
    }

    public float[] getTransitionMatrix() {
        if (surfaceTexture != null) {
            surfaceTexture.getTransformMatrix(transitionMatrix);
        } else {
            Matrix.setIdentityM(transitionMatrix, 0);
        }
        return transitionMatrix;
    }

    public void notifyNoFrame() {
        synchronized (frameSyncObj) {
            frameAvailable = false;
            frameSyncObj.notifyAll();
        }
    }

    public void bind() {
        bind(-1);
    }

    public void bind(int activeId) {

        if (textureId == 0) {
            return;
        }
        if (activeId >= 0) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + activeId);
        }
        GLES20.glBindTexture(textureTarget, textureId);
    }

    public void unbind() {
        unbind(-1);
    }

    public void unbind(int activeId) {
        if (activeId >= 0) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + activeId);
        }
        GLES20.glBindTexture(textureTarget, 0);
    }

    public boolean awaitFrameAvailable() throws InterruptedException {
        return awaitFrameAvailable(0);
    }

    public boolean awaitFrameAvailable(int timeoutMs) throws InterruptedException {
        if (surfaceTexture == null) {
            return true;
        }
        synchronized (frameSyncObj) {
            while (!frameAvailable) {
                try {
                    frameSyncObj.wait(timeoutMs);
                } catch (InterruptedException e) {
                    // shouldn't happen
                    Log.w(TAG, "awaitFrameAvailable with interrupted, it shouldn't happen", e);
                    throw e;
                }
                if (timeoutMs > 0) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        synchronized (frameSyncObj) {
            frameAvailable = true;
            frameSyncObj.notifyAll();
        }
    }

    public int setRenderTarget() {
        if (textureId == 0) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            return GLES20.GL_FRAMEBUFFER_COMPLETE;
        }
        if (framebufferId <= 0) {
            int ids[] = new int[1];
            GLES20.glGenFramebuffers(1, ids, 0);
            framebufferId = ids[0];

        }
        if (!checkFramebuffer(framebufferId)) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebufferId);
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, textureTarget, textureId, 0);
            GLES20.glBindTexture(textureTarget, textureId);
            GLES20.glTexImage2D(textureTarget, 0, GLES20.GL_RGBA, textureWidth, textureHeight, 0,
                    GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebufferId);
        synchronized (RenderTexture.class) {
            RenderTexture.CURRENT_TARGET_TEXTURE = textureId;
        }
        GLES20.glViewport(0, 0, textureWidth, textureHeight);
        return GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
    }

    private boolean checkFramebuffer(int framebufferId) {
        if (framebufferId <= 0) {
            return false;
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebufferId);
        return GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) == GLES20.GL_FRAMEBUFFER_COMPLETE;
    }

    public static void resetRenderTarget() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        synchronized (RenderTexture.class) {
            RenderTexture.CURRENT_TARGET_TEXTURE = 0;
        }
    }

    public int getTextureTarget() {
        return textureTarget;
    }

    public int getTextureId() {
        return textureId;
    }

    public synchronized SurfaceTexture getSurfaceTexture() {
        return surfaceTexture;
    }

    public Surface getSurface() {
        synchronized (this) {
            if (surface == null && surfaceTexture != null) {
                surface = new Surface(surfaceTexture);
            }
        }
        return surface;
    }

    public Bitmap saveFrame(int width, int height) {
        ByteBuffer buf = ByteBuffer.allocate(width * height * 4);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf);
        buf.rewind();

        int pixelCount = width * height;
        int[] colors = new int[pixelCount];
        buf.asIntBuffer().get(colors);
        for (int i = 0; i < pixelCount; i++) {
            int c = colors[i];
            colors[i] = (c & 0xff00ff00) | ((c & 0x00ff0000) >> 16) | ((c & 0x000000ff) << 16);
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream("/sdcard/frame.png");
            Bitmap bmp = Bitmap.createBitmap(colors, width, height, Bitmap.Config.ARGB_8888);
            bmp.compress(Bitmap.CompressFormat.PNG, 90, fos);
            return bmp;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public static String getTargetName(int textureTarget) {
        if (textureTarget == GLES20.GL_TEXTURE_2D) {
            return "TEXTURE_2D";
        } else if (textureTarget == GLES11Ext.GL_TEXTURE_EXTERNAL_OES) {
            return "TEXTURE_EXTERNAL_OES";
        } else {
            return "unknown";
        }
    }
}
