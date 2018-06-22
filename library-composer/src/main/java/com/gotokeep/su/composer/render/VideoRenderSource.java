package com.gotokeep.su.composer.render;

import android.media.MediaExtractor;
import android.media.MediaFormat;

import com.gotokeep.su.composer.decode.DecodeEngine;
import com.gotokeep.su.composer.decode.DecodeRequest;
import com.gotokeep.su.composer.gles.RenderTexture;
import com.gotokeep.su.composer.source.MediaTrack;
import com.gotokeep.su.composer.time.Time;
import com.gotokeep.su.composer.util.Size;

import java.io.IOException;

/**
 * This will be the result for decoding, if want use it in render chain it must be converted
 * to VideoRenderNode through VideoInputNode.
 * @author xana/cuixianming
 * @version 1.0
 * @since 2018/6/21 12:00
 */
public final class VideoRenderSource {
    private MediaTrack sourceTrack;
    private MediaFormat sourceFormat;
    private MediaExtractor sourceExtractor;
    private RenderTexture sourceTexture;
    private Size videoSize;

    public VideoRenderSource(MediaTrack sourceTrack) throws IOException, IllegalArgumentException {
        this.sourceTrack = sourceTrack;
        prepare();
    }

    private void prepare() throws IOException, IllegalArgumentException {
        sourceExtractor = new MediaExtractor();
        sourceExtractor.setDataSource(sourceTrack.getSource().toString());
        sourceExtractor.selectTrack(sourceTrack.getTrackIndex());
        sourceFormat = sourceExtractor.getTrackFormat(sourceTrack.getTrackIndex());
        if (!sourceFormat.containsKey(MediaFormat.KEY_WIDTH) || !sourceFormat.containsKey(MediaFormat.KEY_HEIGHT)) {
            throw new IllegalArgumentException("Media in track does not contain size.");
        }
        videoSize = new Size(sourceFormat.getInteger(MediaFormat.KEY_WIDTH),
                sourceFormat.getInteger(MediaFormat.KEY_HEIGHT));
        sourceTexture = new RenderTexture(RenderTexture.TEXTURE_EXTERNAL, videoSize.getWidth(),
                videoSize.getHeight());
    }

    public MediaTrack getSourceTrack() {
        return sourceTrack;
    }

    public MediaExtractor getSourceExtractor() {
        return sourceExtractor;
    }

    public MediaFormat getSourceFormat() {
        return sourceFormat;
    }

    public RenderTexture getSourceTexture() {
        return sourceTexture;
    }

    public Size getVideoSize() {
        return videoSize;
    }

    public void requestDecode(DecodeEngine decodeEngine, Time renderTimeUs) {
        DecodeRequest request = DecodeRequest.obtain(sourceTrack, renderTimeUs.value);
        decodeEngine.sendVideoRequest(request, sourceTexture.getSurface());
        sourceTexture.awaitFrameAvailable();
    }
}
