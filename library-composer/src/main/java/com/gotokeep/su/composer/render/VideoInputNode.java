package com.gotokeep.su.composer.render;

import android.support.annotation.IntDef;

import com.gotokeep.su.composer.gles.AttributeData;
import com.gotokeep.su.composer.gles.ProgramObject;
import com.gotokeep.su.composer.gles.RenderTexture;
import com.gotokeep.su.composer.gles.RenderUniform;
import com.gotokeep.su.composer.time.Time;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author xana/cuixianming
 * @version 1.0
 * @since 2018/6/21 15:37
 */
public class VideoInputNode extends VideoRenderNode {
    public static final int PREVIEW_MODE = 0;
    public static final int EXPORT_MODE = 1;

    private static final int EXPORT_FRAME_WAIT_TIME_MS = 1000;
    private static final int DECODE_TEXTURE = 0;

    private static final String EXTERNAL_FRAGMENT_SHADER = "" +
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "uniform samplerExternalOES uTexture;\n" +
            "varying vec2 vTexCoords;\n" +
            "void main() { \n" +
            "    gl_FragColor = texture2D(uTexture, vTexCoords);\n" +
            "}\n";
    private final RenderEngine renderEngine;
    private final VideoRenderSource source;
    private final RenderTexture decodeTexture;
    private RenderUniform decodeUniforms[];
    private RenderTexture sourceTextures[];
    @RenderMode
    private int renderMode;
    public VideoInputNode(RenderEngine renderEngine, VideoRenderSource source, @RenderMode int renderMode) {
        super();
        this.renderEngine = renderEngine;
        this.source = source;
        this.decodeTexture = source.getSourceTexture();
        this.sourceTextures = new RenderTexture[]{decodeTexture};
        this.renderMode = renderMode;
        decodeUniforms = new RenderUniform[] {
                new RenderUniform(ProgramObject.UNIFORM_TEXTURE, RenderUniform.TYPE_INT, DECODE_TEXTURE)
        };
    }

    @Override
    public void release() {

    }

    public void setRenderMode(@RenderMode int renderMode) {
        this.renderMode = renderMode;
    }

    @Override
    public void render(Time renderTimeUs) {
        if (decodeTexture == null) {
            return;
        }
        if (renderMode == PREVIEW_MODE) {
            if (!decodeTexture.isFrameAvailable()) {
                targetFrameAvailable = false;
                return;
            }
        } else if (renderMode == EXPORT_MODE) {
            try {
                if (!decodeTexture.awaitFrameAvailable(EXPORT_FRAME_WAIT_TIME_MS)) {
                    targetFrameAvailable = false;
                    return;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (renderProgram != null) {
            RenderRequest request = RenderRequest.obtain(sourceTextures, AttributeData.DEFAULT_ATTRIBUTE_DATA,
                    renderProgram, decodeUniforms, targetTexture, renderTimeUs.value, source.getVideoSize());
            renderEngine.sendRequest(request);
        }
//        source.requestDecode(renderTimeUs);
    }

    @Override
    protected ProgramObject createRenderProgram() {
        return new ProgramObject(EXTERNAL_FRAGMENT_SHADER, ProgramObject.DEFAULT_UNIFORM_NAMES);
    }

    @IntDef({PREVIEW_MODE, EXPORT_MODE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface RenderMode {
    }
}
