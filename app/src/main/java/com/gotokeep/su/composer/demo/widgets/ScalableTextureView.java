package com.gotokeep.su.composer.demo.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;

import com.gotokeep.su.composer.R;
import com.gotokeep.su.composer.demo.scale.MatrixManager;
import com.gotokeep.su.composer.demo.scale.ScaleType;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

/**
 * @author xana/cuixianming
 * @version 1.0
 * @since 2018-02-11 19:33
 */

public class ScalableTextureView extends TextureView implements MatrixManager.MatrixChangeListener {
    private MatrixManager matrixManager;
    private ScaleType scaleType;

    public static final String TAG = ScalableTextureView.class.getSimpleName();

    public ScalableTextureView(Context context) {
        this(context, null);
    }

    public ScalableTextureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScalableTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ScalableTextureView);
        int scaleType = ta.getInt(R.styleable.ScalableTextureView_kvScaleType, 0);
        this.scaleType = ScaleType.fromOrdinal(scaleType);
        ta.recycle();
        init();
    }

    private void init() {
        matrixManager = new MatrixManager(scaleType, this);
        setScaleType(scaleType);
    }

    public void setScaleType(ScaleType scaleType) {
        matrixManager.setScaleType(scaleType);
    }

    public void setVideoSize(int width, int height, int unappliedRotationDegrees) {
        matrixManager.setRotation(unappliedRotationDegrees);
        matrixManager.setVideoSize(width, height);
    }

    public void setVideoMatrix(Matrix matrix) {
        matrixManager.setMatrix(matrix);
    }


    public void setVideoRotation(int unappliedVideoRotation) {
        matrixManager.setRotation(unappliedVideoRotation);
    }

    public Matrix getVideoMatrix() {
        return getTransform(null);
    }

    public Matrix getScaleMatrix() {
        return matrixManager.getScaleMatrix();
    }

    @Override
    public void onMatrixChanged(Matrix matrix) {
        Log.d(TAG, "onMatrixChanged: " + matrix.toString());
        setTransform(matrix);
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        matrixManager.setViewSize(w, h);
    }

    public ScaleType getScaleType() {
        return scaleType;
    }


    /**
     * A version of the EGL14.EGL_CONTEXT_CLIENT_VERSION so that we can
     * reference it without being on API 17+
     */
    private static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;

    /**
     * Because the TextureView itself doesn't contain a method to clear the surface
     * we need to use GL to perform teh clear ourselves.  This means initializing
     * a GL context, and specifying attributes.  This is the attribute list for
     * the configuration of that context
     */
    @SuppressWarnings("MismatchedReadAndWriteOfArray")
    private static final int[] GL_CLEAR_CONFIG_ATTRIBUTES = {
            EGL10.EGL_RED_SIZE, 8,
            EGL10.EGL_GREEN_SIZE, 8,
            EGL10.EGL_BLUE_SIZE, 8,
            EGL10.EGL_ALPHA_SIZE, 8,
            EGL10.EGL_RENDERABLE_TYPE, EGL10.EGL_WINDOW_BIT,
            EGL10.EGL_NONE, 0,
            EGL10.EGL_NONE
    };

    /**
     * Because the TextureView itself doesn't contain a method to clear the surface
     * we need to use GL to perform teh clear ourselves.  This means initializing
     * a GL context, and specifying attributes.  This is the attribute list for
     * that context
     */
    @SuppressWarnings("MismatchedReadAndWriteOfArray")
    private static final int[] GL_CLEAR_CONTEXT_ATTRIBUTES = {
            EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL10.EGL_NONE
    };

    public void clearSurface() {
        if (getSurfaceTexture() == null) {
            return;
        }

        try {
            EGL10 gl10 = (EGL10) EGLContext.getEGL();
            EGLDisplay display = gl10.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
            gl10.eglInitialize(display, null);

            EGLConfig[] configs = new EGLConfig[1];
            gl10.eglChooseConfig(display, GL_CLEAR_CONFIG_ATTRIBUTES, configs, configs.length, new int[1]);
            EGLContext context = gl10.eglCreateContext(display, configs[0], EGL10.EGL_NO_CONTEXT, GL_CLEAR_CONTEXT_ATTRIBUTES);
            EGLSurface eglSurface = gl10.eglCreateWindowSurface(display, configs[0], getSurfaceTexture(), new int[]{EGL10.EGL_NONE});

            gl10.eglMakeCurrent(display, eglSurface, eglSurface, context);
            gl10.eglSwapBuffers(display, eglSurface);
            gl10.eglDestroySurface(display, eglSurface);
            gl10.eglMakeCurrent(display, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
            gl10.eglDestroyContext(display, context);

            gl10.eglTerminate(display);
        } catch (Exception e) {
            Log.e(TAG, "Error clearing surface", e);
        }
    }
}
