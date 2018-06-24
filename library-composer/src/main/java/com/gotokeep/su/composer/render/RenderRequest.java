package com.gotokeep.su.composer.render;

import android.support.annotation.Nullable;
import android.support.v4.util.Pools;

import com.gotokeep.su.composer.gles.AttributeData;
import com.gotokeep.su.composer.gles.ProgramObject;
import com.gotokeep.su.composer.gles.RenderTexture;
import com.gotokeep.su.composer.gles.RenderUniform;
import com.gotokeep.su.composer.util.Size;

import java.util.Map;

/**
 * @author xana/cuixianming
 * @version 1.0
 * @since 2018/6/8 00:06
 */
public final class RenderRequest {
    private static Pools.Pool<RenderRequest> pool = new Pools.SynchronizedPool<>(32);

    public static RenderRequest obtain(RenderTexture sources[], AttributeData renderData, ProgramObject renderProgram,
                                       RenderUniform renderUniforms[], @Nullable RenderTexture targetTexture,
                                       long presentationTimeUs, Size renderSize) {
        RenderRequest request = pool.acquire();
        if (request == null) {
            request = new RenderRequest();
        }
        request.sourceTextures = sources;
        request.renderData = renderData;
        request.renderProgram = renderProgram;
        request.renderUniforms = renderUniforms;
        request.targetTexture = targetTexture;
        request.renderSize = renderSize;
        request.presentationTimeUs = presentationTimeUs;
        request.recycled = false;
        return request;
    }

    public void recycle() {
        if (!recycled) {
            pool.release(this);
        }
    }

    public RenderTexture sourceTextures[];
    public AttributeData renderData;
    public ProgramObject renderProgram;
    public RenderUniform[] renderUniforms;
    public RenderTexture targetTexture;
    public Size renderSize;
    public long presentationTimeUs;
    public boolean recycled = false;
}
