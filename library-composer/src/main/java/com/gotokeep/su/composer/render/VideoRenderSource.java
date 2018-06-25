package com.gotokeep.su.composer.render;

import android.media.MediaExtractor;
import android.media.MediaFormat;

import com.gotokeep.su.composer.decode.DecodeEngine;
import com.gotokeep.su.composer.decode.DecodeRequest;
import com.gotokeep.su.composer.gles.RenderTexture;
import com.gotokeep.su.composer.composition.CompositionSegment;
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
    private CompositionSegment sourceTrackSegment;
    private MediaFormat sourceFormat;
    private MediaExtractor sourceExtractor;
    private RenderTexture sourceTexture;
    private Size videoSize;

    public VideoRenderSource(CompositionSegment sourceTrackSegment) throws IOException, IllegalArgumentException {
        this.sourceTrackSegment = sourceTrackSegment;
        prepare();
    }

    private void prepare() throws IOException, IllegalArgumentException {
        sourceExtractor = new MediaExtractor();
        sourceExtractor.setDataSource(sourceTrackSegment.getSourceTrack().getSource().toString());
        sourceExtractor.selectTrack(sourceTrackSegment.getSourceTrack().getTrackIndex());
        sourceFormat = sourceExtractor.getTrackFormat(sourceTrackSegment.getSourceTrack().getTrackIndex());
        if (!sourceFormat.containsKey(MediaFormat.KEY_WIDTH) || !sourceFormat.containsKey(MediaFormat.KEY_HEIGHT)) {
            throw new IllegalArgumentException("Media in track does not contain size.");
        }
        videoSize = new Size(sourceFormat.getInteger(MediaFormat.KEY_WIDTH),
                sourceFormat.getInteger(MediaFormat.KEY_HEIGHT));
        sourceTexture = new RenderTexture(RenderTexture.TEXTURE_EXTERNAL, videoSize.getWidth(),
                videoSize.getHeight());
    }

    public CompositionSegment getSourceTrack() {
        return sourceTrackSegment;
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
        DecodeRequest request = DecodeRequest.obtain(sourceTrackSegment, sourceExtractor, sourceTexture.getSurface(), renderTimeUs.value);
        decodeEngine.sendVideoRequest(request);
        try {
            sourceTexture.awaitFrameAvailable();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
