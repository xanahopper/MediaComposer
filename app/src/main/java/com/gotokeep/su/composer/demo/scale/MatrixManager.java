package com.gotokeep.su.composer.demo.scale;

import android.graphics.Matrix;
import android.graphics.RectF;

/**
 * @author xana/cuixianming
 * @version 1.0
 * @since 2018-02-11 19:35
 */

public class MatrixManager {
    private Matrix scaleMatrix;
    private Matrix matrix;
    private Matrix drawMatrix;
    private ScaleType scaleType = ScaleType.CENTER_CROP;

    private int rotation = 0;
    private int viewWidth = 0;
    private int viewHeight = 0;
    private int videoWidth = 0;
    private int videoHeight = 0;

    private RectF tmpSrc = new RectF();
    private RectF tmpDst = new RectF();

    private MatrixChangeListener matrixChangeListener;

    public MatrixManager(ScaleType scaleType, MatrixChangeListener listener) {
        this.matrixChangeListener = listener;
        this.scaleType = scaleType;

        scaleMatrix = new Matrix();
        matrix = new Matrix();
        drawMatrix = new Matrix();
    }

    public void setVideoSize(int width, int height) {
        videoWidth = width;
        videoHeight = height;
        updateMatrix();
    }

    public void setViewSize(int width, int height) {
        viewWidth = width;
        viewHeight = height;
        updateMatrix();
    }

    public void setMatrix(Matrix matrix) {
        if (matrix != null && matrix.isIdentity()) {
            matrix = null;
        }

        if (matrix == null && !this.matrix.isIdentity() ||
                matrix != null && !this.matrix.equals(matrix)) {
            this.matrix.set(matrix);
            updateMatrix();
        }
    }

    public void setScaleType(ScaleType scaleType) {
        if (scaleType == null) {
            throw new NullPointerException();
        }

        if (this.scaleType != scaleType) {
            this.scaleType = scaleType;
            updateMatrix();
        }
    }

    public ScaleType getScaleType() {
        return scaleType;
    }

    public Matrix getScaleMatrix() {
        return scaleMatrix;
    }

    public Matrix getDrawMatrix() {
        return drawMatrix;
    }

    private void updateMatrix() {
        final boolean isRotate = (rotation % 180 == 90);
        final boolean fits = (videoWidth <= 0 || viewWidth == videoWidth)
                && (videoHeight <= 0 || viewHeight == videoHeight);

        if (videoWidth > 0 && videoHeight > 0 && viewWidth > 0 && viewHeight > 0) {
            scaleMatrix.reset();
            scaleMatrix.setScale((float) videoWidth / (float) viewWidth,
                    (float) videoHeight / (float) viewHeight);
            scaleMatrix.postRotate(rotation % 180, videoHeight / 2, videoHeight / 2);
            if (rotation > 180) {
                scaleMatrix.postRotate(180, videoHeight / 2,  videoWidth / 2);
            }
        }

        if (isRotate) {
            int t = videoWidth;
            videoWidth = videoHeight;
            videoHeight = t;
        }

        if (scaleType == ScaleType.MATRIX) {
            if (matrix.isIdentity()) {
                drawMatrix.reset();
            } else {
                drawMatrix.set(matrix);
            }
        } else if (fits) {
            drawMatrix.reset();
        } else if (scaleType == ScaleType.CENTER) {
            drawMatrix.set(scaleMatrix);
            drawMatrix.setTranslate(Math.round((viewWidth - videoWidth) * 0.5f),
                                    Math.round((viewHeight - videoHeight) * 0.5f));
        } else if (scaleType == ScaleType.CENTER_CROP) {
            drawMatrix.set(scaleMatrix);
            float scale;
            float dx = 0, dy = 0;

            if (videoWidth * viewHeight > viewWidth * videoHeight) {
                scale = (float) viewHeight / (float) videoHeight;
                dx = (viewWidth - videoWidth * scale) * 0.5f;
            } else {
                scale = (float) viewWidth / (float) videoWidth;
                dy = (viewHeight - videoHeight * scale) * 0.5f;
            }
            drawMatrix.postScale(scale, scale);
            drawMatrix.postTranslate(Math.round(dx), Math.round(dy));
        } else if (scaleType == ScaleType.CENTER_INSIDE) {
            drawMatrix.set(scaleMatrix);
            float scale;
            float dx, dy;

            if (videoWidth <= viewWidth && videoHeight <= viewHeight) {
                scale = 1.0f;
            } else {
                scale = Math.min((float) viewWidth / (float) videoWidth,
                        (float) viewHeight / (float) videoHeight);
            }

            dx = Math.round((viewWidth - videoWidth * scale) * 0.5f);
            dy = Math.round((viewHeight - videoHeight * scale) * 0.5f);

            drawMatrix.postScale(scale, scale);
            drawMatrix.postTranslate(dx, dy);
        } else {
            tmpSrc.set(0, 0, videoWidth, videoHeight);
            tmpDst.set(0, 0, viewWidth, viewHeight);
            drawMatrix.set(scaleMatrix);
            Matrix tmp = new Matrix();
            if (scaleType == ScaleType.FIT_CENTER) {
                tmp.setRectToRect(tmpSrc, tmpDst, Matrix.ScaleToFit.CENTER);
            } else if (scaleType == ScaleType.FIT_XY) {
                tmp.setRectToRect(tmpSrc, tmpDst, Matrix.ScaleToFit.FILL);
            }
            drawMatrix.postConcat(tmp);
        }
        matrixChangeListener.onMatrixChanged(drawMatrix);
    }

    public void setRotation(int unappliedVideoRotation) {
        rotation = unappliedVideoRotation;
    }

    public interface MatrixChangeListener {
        void onMatrixChanged(Matrix matrix);
    }
}
