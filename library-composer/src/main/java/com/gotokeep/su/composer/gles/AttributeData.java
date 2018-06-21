package com.gotokeep.su.composer.gles;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * @author xana/cuixianming
 * @version 1.0
 * @since 2018/6/21 16:16
 */
public final class AttributeData {
    private static final float[] DEFAULT_VERTEX_DATA = {
            -1f, -1f, 0,
            1f, -1f, 0,
            -1f, 1f, 0,
            1f, 1f, 0};
    private static final short[] DEFAULT_TEX_COORDS_DATA = {0, 0, 1, 0, 0, 1, 1, 1};
    public static final AttributeData DEFAULT_ATTRIBUTE_DATA = new AttributeData();

    private int vertexCount = 0;
    protected FloatBuffer vertexBuffer;
    protected ShortBuffer texCoordBuffer;

    public AttributeData() {
        this(DEFAULT_VERTEX_DATA, DEFAULT_TEX_COORDS_DATA);
    }

    public AttributeData(short[] texCoordsData) {
        this(DEFAULT_VERTEX_DATA, texCoordsData);
    }

    public AttributeData(float[] vertexData) {
        this(vertexData, DEFAULT_TEX_COORDS_DATA);
    }

    public AttributeData(float[] vertexData, short[] texCoordsData) {
        vertexBuffer = ByteBuffer.allocateDirect(vertexData.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        vertexBuffer.put(vertexData).position(0);
        vertexCount = vertexData.length / 3;

        texCoordBuffer = ByteBuffer.allocateDirect(texCoordsData.length * 2)
                .order(ByteOrder.nativeOrder()).asShortBuffer();
        texCoordBuffer.put(texCoordsData).position(0);
    }

    public void enable() {
        GLES20.glVertexAttribPointer(0, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glEnableVertexAttribArray(0);
        GLES20.glVertexAttribPointer(1, 2, GLES20.GL_SHORT, false, 0, texCoordBuffer);
        GLES20.glEnableVertexAttribArray(1);
    }

    public void disable() {
        GLES20.glDisableVertexAttribArray(0);
        GLES20.glDisableVertexAttribArray(1);
    }

    public void draw() {
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, vertexCount);
    }
}
