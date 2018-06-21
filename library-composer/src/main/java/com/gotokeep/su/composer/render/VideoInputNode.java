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

    @IntDef({PREVIEW_MODE, EXPORT_MODE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface RenderMode {
    }

    private final VideoRenderSource source;
    private final RenderTexture decodeTexture;
    private RenderUniform decodeUniform;
    @RenderMode
    private int renderMode;

    public VideoInputNode(VideoRenderSource source, @RenderMode int renderMode) {
        super();
        this.source = source;
        this.decodeTexture = source.getSourceTexture();
        this.renderMode = renderMode;
        decodeUniform = new RenderUniform(RenderUniform.TYPE_INT, DECODE_TEXTURE);
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
            if (!decodeTexture.awaitFrameAvailable(EXPORT_FRAME_WAIT_TIME_MS)) {
                targetFrameAvailable = false;
                return;
            }
        }
        if (renderProgram != null) {
            renderProgram.use(AttributeData.DEFAULT_ATTRIBUTE_DATA);
            decodeTexture.bind(DECODE_TEXTURE);
            renderProgram.setUniform(ProgramObject.UNIFORM_TEXTURE, decodeUniform);
            AttributeData.DEFAULT_ATTRIBUTE_DATA.draw();
            targetFrameAvailable = true;
        }
        source.requestDecode(renderTimeUs);
    }

    @Override
    protected ProgramObject createRenderProgram() {
        return new ProgramObject(EXTERNAL_FRAGMENT_SHADER, ProgramObject.DEFAULT_UNIFORM_NAMES);
    }
}
