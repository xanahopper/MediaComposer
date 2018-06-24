package com.gotokeep.su.composer.gles;

/**
 * @author xana/cuixianming
 * @version 1.0
 * @since 2018/6/7 23:59
 */
public final class RenderUniform {
    public static final int TYPE_INT = 0;
    public static final int TYPE_FLOAT = 1;
    public static final int TYPE_INT_ARRAY = 2;
    public static final int TYPE_FLOAT_ARRAY = 3;
    public static final int TYPE_MATRIX = 4;

    public String name;
    public int type;
    private Object data;

    public RenderUniform(String name, int type, Object data) {
        this.name = name;
        this.type = type;
        this.data = data;
    }

    public RenderUniform(String name, int data) {
        this(name, TYPE_INT, data);
    }

    public RenderUniform(String name, float data) {
        this(name, TYPE_FLOAT, data);
    }

    public RenderUniform(String name, int[] data) {
        this(name, TYPE_INT_ARRAY, data);
    }

    public RenderUniform(String name, float[] data) {
        this(name, TYPE_FLOAT_ARRAY, data);
    }

    public int getIntData() {
        return (int) data;
    }

    public int[] getIntArrayData() {
        return (int[]) data;
    }

    public float getFloatData() {
        return (float) data;
    }

    public float[] getFloatArrayData() {
        return (float[]) data;
    }

    public float[] getMatrixData() {
        return getFloatArrayData();
    }
}
