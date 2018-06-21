package com.gotokeep.su.composer.render;

import com.gotokeep.su.composer.gles.ProgramObject;
import com.gotokeep.su.composer.gles.RenderTexture;
import com.gotokeep.su.composer.gles.RenderUniform;

import java.util.Map;

/**
 * @author xana/cuixianming
 * @version 1.0
 * @since 2018/6/8 00:06
 */
public final class RenderRequest {
    public RenderTexture targetTexture;
    public RenderTexture sourceTextures[];
    public ProgramObject renderProgram;
    public Map<String, RenderUniform> renderUniforms;
}
