package com.gotokeep.su.composer.gles;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import java.util.Map;
import java.util.TreeMap;

/**
 * @author xana/cuixianming
 * @version 1.0
 * @since 2018/5/13 18:43
 */
public final class ProgramObject {
    public static final String ATTRIBUTE_POSITION = "aPosition";
    public static final String ATTRIBUTE_TEX_COORDS = "aTexCoords";
    public static final String UNIFORM_TEXCOORD_MATRIX = "uTexCoords";
    public static final String UNIFORM_TRANSFORM_MATRIX = "uTransformMatrix";
    public static final String UNIFORM_TEXTURE = "uTexture";
    public static final String DEFAULT_UNIFORM_NAMES[] = {
            UNIFORM_TRANSFORM_MATRIX,
            UNIFORM_TEXCOORD_MATRIX,
            UNIFORM_TEXTURE
    };
    private static final String TAG = ProgramObject.class.getSimpleName();
    private final String vertexShader;
    private final String fragmentShader;

    private int programId = -1;
    private boolean attribBinded = false;

    private static final String DEFAULT_VERTEX_SHADER = "" +
            "attribute vec4 aPosition;    \n" +
            "attribute vec2 aTexCoords; \n" +
            "varying vec2 vTexCoords; \n" +
            "uniform mat4 uTexCoords;\n" +
            "uniform mat4 uTransformMatrix;\n" +
            "void main()                  \n" +
            "{                            \n" +
            "    gl_Position = uTransformMatrix * aPosition;  \n" +
            "    vTexCoords = (uTexCoords * vec4(aTexCoords, 0.0, 1.0)).st; \n" +
            "}                            \n";

    public static final String DEFAULT_FRAGMENT_SHADER = "" +
            "precision mediump float;\n" +
            "uniform sampler2D uTexture;\n" +
            "varying vec2 vTexCoords;\n" +
            "void main() { \n" +
            "    gl_FragColor = texture2D(uTexture, vTexCoords);\n" +
            "}\n";

    public static final float[] DEFAULT_MATRIX = {
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f
    };

    private Map<String, Integer> uniforms;

    private volatile static ProgramObject defaultProgram = null;

    public static ProgramObject getDefaultProgram() {
        if (defaultProgram == null) {
            synchronized (ProgramObject.class) {
                if (defaultProgram == null) {
                    defaultProgram = new ProgramObject();
                }
            }
        }
        return defaultProgram;
    }

    static {
        Matrix.setIdentityM(DEFAULT_MATRIX, 0);
    }

    public ProgramObject() {
        this(DEFAULT_FRAGMENT_SHADER, DEFAULT_UNIFORM_NAMES);
    }

    public ProgramObject(String fragmentShader, String uniformNames[]) {
        this(DEFAULT_VERTEX_SHADER, fragmentShader, uniformNames);
    }

    public ProgramObject(String vertexShader, String fragmentShader, String uniformNames[]) {
        this.vertexShader = vertexShader;
        this.fragmentShader = fragmentShader;
        programId = createProgram(vertexShader, fragmentShader);
        if (programId < 0) {
            Log.w(TAG, "program created failed");
        }
        initUniformLocations(uniformNames);
        initDefaultTransform();
    }

    public void use() {
        GLES20.glUseProgram(programId);
      //checkGlError("useProgram:" + programId);
    }

    public void use(AttributeData attributeData) {
        use();
        if (!attribBinded) {
            GLES20.glBindAttribLocation(programId, 0, ProgramObject.ATTRIBUTE_POSITION);
            GLES20.glBindAttribLocation(programId, 1, ProgramObject.ATTRIBUTE_TEX_COORDS);
            attributeData.enable();
            attribBinded = true;
        } else {
            attributeData.enable();
        }
    }

    public int getProgramId() {
        return programId;
    }

    private int createProgram(String vertexShader, String fragmentShader) {
        return OpenGlUtils.loadProgram(vertexShader, fragmentShader);
    }

    private void initUniformLocations(String[] uniformNames) {
        uniforms = new TreeMap<>();
        if (uniformNames != null) {
            for (String uniformName : uniformNames) {
                int loc = GLES20.glGetUniformLocation(programId, uniformName);
              //checkGlError("getUniformLocation(" + programId + ", " + uniformName + ") = " + loc);
                if (loc == -1) {
                    Log.e("ProgramObject", "initUniformLocations: invalid uniform location for " + uniformName +
                                    " : " + GLES20.glGetError());
                }
                uniforms.put(uniformName, loc);
            }
        }
    }

    private void initDefaultTransform() {
        use();
        int loc = getUniformLocation(UNIFORM_TEXCOORD_MATRIX);
        float st[] = new float[16];
        Matrix.setIdentityM(st, 0);
        GLES20.glUniformMatrix4fv(loc, 1, false, st, 0);
        loc = getUniformLocation(UNIFORM_TRANSFORM_MATRIX);
        GLES20.glUniformMatrix4fv(loc, 1, false, st, 0);

      //checkGlError("uniformMatrix4fv: " + loc);
    }

    public int getUniformLocation(String name) {
        return uniforms.containsKey(name) ? uniforms.get(name) : -1;
    }

    public void setUniform(RenderUniform uniform) {
        int loc = getUniformLocation(uniform.name);
        if (loc >= 0) {
            switch (uniform.type) {
                case RenderUniform.TYPE_INT:
                    GLES20.glUniform1i(loc, uniform.getIntData());
                    break;
                case RenderUniform.TYPE_FLOAT:
                    GLES20.glUniform1f(loc, uniform.getFloatData());
                    break;
                case RenderUniform.TYPE_INT_ARRAY:
                    int intData[] = uniform.getIntArrayData();
                    switch (intData.length) {
                        case 2:
                            GLES20.glUniform2iv(loc, 1, intData, 0);
                            break;
                        case 3:
                            GLES20.glUniform3iv(loc, 1, intData, 0);
                            break;
                        case 4:
                            GLES20.glUniform4iv(loc, 1, intData, 0);
                            break;
                    }
                    break;
                case RenderUniform.TYPE_FLOAT_ARRAY:
                case RenderUniform.TYPE_MATRIX:
                    float floatData[] = uniform.getFloatArrayData();
                    switch (floatData.length) {
                        case 2:
                            GLES20.glUniform2fv(loc, 1, floatData, 0);
                            break;
                        case 3:
                            GLES20.glUniform3fv(loc, 1, floatData, 0);
                            break;
                        case 4:
                            GLES20.glUniform4fv(loc, 1, floatData, 0);
                            break;
                        case 9:
                            GLES20.glUniformMatrix3fv(loc, 1, false, floatData, 0);
                        case 16:
                            GLES20.glUniformMatrix4fv(loc, 1, false, floatData, 0);
                            break;
                    }
                    break;
            }
        }
    }

    public void release() {
        if (defaultProgram == null || programId != defaultProgram.programId) {
            GLES20.glDeleteProgram(programId);
          //checkGlError("deleteProgram: " + programId);
        }
        programId = -1;
    }
}