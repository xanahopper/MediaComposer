package com.gotokeep.su.composer.demo.sample;

import android.graphics.SurfaceTexture;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import com.gotokeep.su.composer.demo.SampleActivity;
import com.gotokeep.su.composer.demo.source.SourceProvider;
import com.gotokeep.su.composer.gles.AttributeData;
import com.gotokeep.su.composer.gles.EglCore;
import com.gotokeep.su.composer.gles.ProgramObject;
import com.gotokeep.su.composer.gles.RenderTexture;
import com.gotokeep.su.composer.gles.RenderUniform;
import com.gotokeep.su.composer.time.Time;
import com.gotokeep.su.composer.time.TimeRange;
import com.gotokeep.su.composer.util.MediaClock;
import com.gotokeep.su.composer.util.MediaUtil;
import com.gotokeep.su.composer.util.TimeUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

/**
 * @author xana/cuixianming
 * @version 1.0
 * @since 2018-06-22 18:15
 */
public class CommonDecodeActivity extends SampleActivity implements TextureView.SurfaceTextureListener {
    private static final String TAG = CommonDecodeActivity.class.getSimpleName();
    public static final int TIMEOUT_US = 1000;
    private MediaClock playClock = new MediaClock();
    private MediaClock audioClock = new MediaClock();
    private Decoder videoDecoder = new Decoder();
    private Decoder audioDecoder = new Decoder();
    private Semaphore videoSem = new Semaphore(1);
    private Semaphore audioSem = new Semaphore(1);

    private Map<String, MediaExtractor> videoExtractors = new HashMap<>();
    private Map<String, MediaExtractor> audioExtractors = new HashMap<>();
    private List<TrackSource> videoTrackSources = new ArrayList<>();
    private List<TrackSource> audioTrackSources = new ArrayList<>();

    private Map<TrackSource, SourceState> stateMap = new HashMap<>();

    private EglCore eglCore;
    private EGLSurface eglSurface;
    private Surface outputSurface;
    private Surface decodeSurface;
    private RenderTexture decodeTexture;
    private ProgramObject renderProgram;
    private AttributeData renderAttribData = new AttributeData();
    private RenderUniform textureUniform = new RenderUniform(ProgramObject.UNIFORM_TEXTURE, RenderUniform.TYPE_INT, 0);
    private RenderUniform transformMatrixUniform;

    private RenderThread renderThread;
    private HandlerThread videoThread;
    private VideoHandler videoHandler;

    private HandlerThread audioThread;
    private AudioHandler audioHandler;

    private HandlerThread playThread;
    private Handler playHandler;
    private PlayHandlerCallback playCallback = new PlayHandlerCallback();

    private static final String EXTERNAL_FRAGMENT_SHADER = "" +
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "uniform samplerExternalOES uTexture;\n" +
            "varying vec2 vTexCoords;\n" +
            "void main() { \n" +
            "    gl_FragColor = texture2D(uTexture, vTexCoords);\n" +
            "}\n";

    private static final int MSG_SETUP = 861;
    private static final int MSG_SET_SURFACE = 879;
    private static final int MSG_PREPARE = 154;
    private static final int MSG_PLAY = 706;
    private static final int MSG_DO_NEXT = 722;
    private static final int MSG_QUIT = 763;
    private static final int MSG_VIDEO_RENDERED = 831;
    private static final int MSG_AUDIO_RENDERED = 757;
    private static final int MSG_AUDIO_EOS = 970;
    private static final int MSG_VIDEO_EOS = 699;
    private static final int MSG_FRAME_AVAILABLE = 169;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        previewView.setSurfaceTextureListener(this);
        playThread = new HandlerThread("PlayThread");
        playThread.start();
        playHandler = new Handler(playThread.getLooper(), playCallback);

        audioThread = new HandlerThread("AudioThread");
        audioThread.start();
        audioHandler = new AudioHandler(audioThread.getLooper());

        videoThread = new HandlerThread("VideoThread");
        videoThread.start();
        videoHandler = new VideoHandler(videoThread.getLooper());

        renderThread = new RenderThread();

        playHandler.sendEmptyMessage(MSG_PREPARE);
        playHandler.sendEmptyMessage(MSG_PLAY);
    }

    @Override
    protected void onDestroy() {
        videoThread.quit();
        audioThread.quit();
        playThread.quit();
        super.onDestroy();
    }


    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        videoHandler.obtainMessage(MSG_SET_SURFACE, width, height, new Surface(surface)).sendToTarget();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    private class PlayHandlerCallback implements Handler.Callback {
        int sourceIndex = 0;
        long minTimeUs = 0;
        long videoTimeUs = 0;
        long audioTimeUs = 0;

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_PREPARE:
                    prepare();
                    break;
                case MSG_PLAY:
                    play();
                    break;
                case MSG_AUDIO_EOS:
                    sourceIndex += 1;
                    if (sourceIndex < audioTrackSources.size()) {
                        audioHandler.obtainMessage(MSG_DO_NEXT, audioTrackSources.get(sourceIndex)).sendToTarget();
                    }
                    break;
                case MSG_AUDIO_RENDERED:
                    syncAudio((Decoder) msg.obj);
                    break;
                case MSG_VIDEO_RENDERED:
                    syncVideo((Decoder) msg.obj);
                    break;
            }
            return false;
        }

        private void syncVideo(Decoder videoDecoder) {
            MediaCodec.BufferInfo info = videoDecoder.bufferInfo;
            videoTimeUs = info.presentationTimeUs;
            if (videoTimeUs <= audioTimeUs) {
                Log.d(TAG, "syncVideo: " + videoTimeUs + ", " + audioTimeUs);
                videoSem.release();
            }
        }

        private void syncAudio(Decoder audioDecoder) {
            MediaCodec.BufferInfo info = audioDecoder.bufferInfo;
            audioTimeUs = info.presentationTimeUs;
            if (videoTimeUs <= info.presentationTimeUs) {
                videoSem.release();
            }
            audioSem.release();
        }

        private void prepare() {
            for (int i = 0; i < SourceProvider.VIDEO_SRC.length; i++) {
                MediaExtractor videoExtractor = new MediaExtractor();
                MediaExtractor audioExtractor = new MediaExtractor();
                TrackSource videoTrackSource = null;
                TrackSource audioTrackSource = null;
                int videoIndex = -1;
                int audioIndex = -1;
                String videoMime = null;
                String audioMime = null;
                try {
                    String source = SourceProvider.VIDEO_SRC[i];
                    videoExtractor.setDataSource(source);
                    audioExtractor.setDataSource(source);
                    for (int j = 0; j < videoExtractor.getTrackCount(); j++) {
                        MediaFormat format = videoExtractor.getTrackFormat(j);
                        String mime = format.getString(MediaFormat.KEY_MIME);
                        if (mime.startsWith("video/")) {
                            videoIndex = j;
                            videoMime = mime;
                        } else if (mime.startsWith("audio/")) {
                            audioIndex = j;
                            audioMime = mime;
                        }
                    }
                    if (videoIndex >= 0) {
                        videoTrackSource = new TrackSource(source, videoIndex, videoMime, videoExtractor.getTrackFormat(videoIndex));
                        videoExtractor.selectTrack(videoIndex);
                    }
                    if (audioIndex >= 0) {
                        audioTrackSource = new TrackSource(source, audioIndex, audioMime, audioExtractor.getTrackFormat(audioIndex));
                        audioExtractor.selectTrack(audioIndex);
                    }
                    if (videoTrackSource != null) {
                        videoExtractors.put(videoTrackSource.source, videoExtractor);
                        stateMap.put(videoTrackSource, new SourceState(videoTrackSource));
                        Log.d(TAG, "videoTrackSource[" + i + "]: " + videoTrackSource.toString());
                    }
                    videoTrackSources.add(videoTrackSource);
                    if (audioTrackSource != null) {
                        audioExtractors.put(audioTrackSource.source, audioExtractor);
                        stateMap.put(audioTrackSource, new SourceState(audioTrackSource));
                        Log.d(TAG, "audioTrackSource[" + i + "]: " + audioTrackSource.toString());
                    }
                    audioTrackSources.add(audioTrackSource);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void play() {
            playClock.start();
            videoHandler.obtainMessage(MSG_DO_NEXT, videoTrackSources.get(sourceIndex)).sendToTarget();
            audioHandler.obtainMessage(MSG_DO_NEXT, audioTrackSources.get(sourceIndex)).sendToTarget();
        }
    }

    private class RenderThread extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    decodeTexture.awaitFrameAvailable();
                    Log.d(TAG, "Frame");
                    videoHandler.obtainMessage(MSG_FRAME_AVAILABLE, decodeTexture).sendToTarget();
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }

    private class VideoHandler extends Handler {
        TrackSource trackSource;
        public VideoHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SETUP:
                    setup();
                    break;
                case MSG_SET_SURFACE:
                    setSurfaceInternal((Surface) msg.obj, msg.arg1, msg.arg2);
                    break;
                case MSG_DO_NEXT:
                    decode((TrackSource) msg.obj);
                    break;
                case MSG_FRAME_AVAILABLE:
                    render((RenderTexture) msg.obj);
                    break;
                case MSG_QUIT:
                    removeCallbacksAndMessages(null);
                    return;
            }
            super.handleMessage(msg);
        }

        private void render(RenderTexture texture) {

            if (texture.isFrameAvailable()) {
                Log.d(TAG, "render Frame");
                float texCoords[] = new float[16];
                Matrix.setIdentityM(texCoords, 0);
                texture.updateTexImage();
                texture.bind(0);
                renderProgram.use(renderAttribData);
                renderProgram.setUniform(textureUniform);
                renderProgram.setUniform(new RenderUniform(ProgramObject.UNIFORM_TEXCOORD_MATRIX, RenderUniform.TYPE_MATRIX, texCoords));
                renderProgram.setUniform(new RenderUniform(ProgramObject.UNIFORM_TRANSFORM_MATRIX, RenderUniform.TYPE_MATRIX, texture.getTransitionMatrix()));

                renderAttribData.draw();
                eglCore.swapBuffers(eglSurface);
            }
            playHandler.obtainMessage(MSG_VIDEO_RENDERED, videoDecoder).sendToTarget();
        }

        private void decode(TrackSource trackSource) {
            if (trackSource != null) {
                if (this.trackSource != trackSource) {
                    this.trackSource = trackSource;
                    // TODO: some like AudioTrack for video (maybe a renderer?)

                    MediaFormat format = trackSource.format;
                    int width = format.getInteger(MediaFormat.KEY_WIDTH);
                    int height = format.getInteger(MediaFormat.KEY_HEIGHT);
                    decodeTexture.setSize(width, height);

                    if (videoDecoder.state == Decoder.STATE_UNINITIALIZED) {
                        setupDecoder(videoDecoder, trackSource);
                    }
                    if (videoDecoder.mimeType == null || !videoDecoder.mimeType.equals(trackSource.mimeType)) {
                        if (videoDecoder.state != Decoder.STATE_UNINITIALIZED) {
                            videoDecoder.decoder.release();
                            videoDecoder.state = Decoder.STATE_UNINITIALIZED;
                        }
                        setupDecoder(videoDecoder, trackSource);
                    }
                    if (videoDecoder.state == Decoder.STATE_INITIALIZED) {
                        videoDecoder.decoder.configure(trackSource.format, decodeSurface, null, 0);
                        videoDecoder.state = Decoder.STATE_CONFIGURED;
                    }
                    if (videoDecoder.state == Decoder.STATE_CONFIGURED) {
                        videoDecoder.decoder.start();
                        videoDecoder.state = Decoder.STATE_STARTED;
                    }
                }
                if (videoDecoder.state != Decoder.STATE_STARTED) {
                    throw new RuntimeException("AudioDecoder init failed.");
                }

                SourceState state = stateMap.get(trackSource);
                MediaExtractor extractor = videoExtractors.get(trackSource.source);
                MediaCodec decoder = videoDecoder.decoder;

                if (extractor != null && state != null) {
                    int inputIndex;
                    int inputCount = 0;
                    do {
                        inputIndex = -1;
                        if (state.timeRangeMs.isTimeInRange(state.positionMs) && !state.ended) {
                            inputIndex = decoder.dequeueInputBuffer(TIMEOUT_US);
                            if (inputIndex >= 0) {
                                ByteBuffer buffer = MediaUtil.getInputBuffer(decoder, inputIndex);
                                int buffSize = extractor.readSampleData(buffer, 0);
                                long time = extractor.getSampleTime();
                                int flags = extractor.getSampleFlags();
//                                Log.d(TAG, "sampleTime: " + time);
                                if (buffSize > 0) {
                                    decoder.queueInputBuffer(inputIndex, 0, buffSize, time, flags);
                                    inputCount += 1;
                                } else {
                                    decoder.queueInputBuffer(inputIndex, 0, 0, 0, 0);
                                    playHandler.sendEmptyMessage(MSG_VIDEO_EOS);
                                }
                            }
                            if (extractor.advance()) {
                                state.positionMs.value = TimeUtil.usToMs(extractor.getSampleTime());
                                state.ended = false;
                            } else {
                                state.ended = true;
                            }
                        }
                    } while (inputIndex >= 0 || state.ended);
                    boolean decoded = false;
                    while (!decoded) {
                        int outputIndex = decoder.dequeueOutputBuffer(videoDecoder.bufferInfo, TIMEOUT_US);
                        switch (outputIndex) {
                            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                                break;
                            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                                videoDecoder.outputFormat = decoder.getOutputFormat();
                                break;
                            case MediaCodec.INFO_TRY_AGAIN_LATER:
                                decoded = true;
                                break;
                            default:
                                videoDecoder.presentationTimeUs = videoDecoder.bufferInfo.presentationTimeUs;
                                playHandler.obtainMessage(MSG_VIDEO_RENDERED, videoDecoder).sendToTarget();
                                try {
                                    videoSem.acquire();
                                } catch (InterruptedException e) {
                                    return;
                                }
                                decoder.releaseOutputBuffer(outputIndex, true);
                                // here will
                                decoded = true;
                                break;
                        }
                    }

                    if (videoDecoder.presentationTimeUs < state.durationMs.value && !state.ended) {
                        videoHandler.obtainMessage(MSG_DO_NEXT, trackSource).sendToTarget();
                    }
                }
            }
        }

        private void setup() {
            eglCore = new EglCore();
            if (previewView.isAvailable()) {
                videoHandler.obtainMessage(MSG_SET_SURFACE, previewView.getWidth(), previewView.getHeight(), new Surface(previewView.getSurfaceTexture())).sendToTarget();
            } else {
                videoHandler.obtainMessage(MSG_SET_SURFACE, 1, 1,null).sendToTarget();
            }
        }

        private void setSurfaceInternal(Surface surface, int width, int height) {
            if (eglCore == null) {
                videoHandler.sendEmptyMessage(MSG_SETUP);
                return;
            }
            if (eglSurface != null) {
                eglCore.releaseSurface(eglSurface);
            }
            if (outputSurface != null && outputSurface != surface) {
                outputSurface.release();
                outputSurface = null;
            }
            if (surface == null) {
                eglSurface = eglCore.createOffscreenSurface(1, 1);
            } else {
                outputSurface = surface;
                eglSurface = eglCore.createWindowSurface(surface);
            }
            eglCore.makeCurrent(eglSurface);
            GLES20.glViewport(0, 0, width, height);
            if (decodeTexture == null) {
                decodeTexture = new RenderTexture(RenderTexture.TEXTURE_EXTERNAL, 1, 1);
                decodeSurface = new Surface(decodeTexture.getSurfaceTexture());
                renderProgram = new ProgramObject(EXTERNAL_FRAGMENT_SHADER, ProgramObject.DEFAULT_UNIFORM_NAMES);
                renderThread.start();
            }
        }
    }

    private class AudioHandler extends Handler {
        long offsetTime = 0;
        AudioTrack audioTrack;
        TrackSource trackSource;
        AudioHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_DO_NEXT:
                    render((TrackSource) msg.obj);
                    break;
                case MSG_QUIT:
                    removeCallbacksAndMessages(null);
                    return;
            }
            super.handleMessage(msg);
        }

        private void render(TrackSource trackSource) {
            if (trackSource != null) {
                long operationStartTimeUs = TimeUtil.msToUs(SystemClock.elapsedRealtime());
                if (this.trackSource != trackSource) {
                    this.trackSource = trackSource;
                    if (audioTrack != null) {
                        audioTrack.stop();
                        audioTrack.release();
                    }
                    MediaFormat format = trackSource.format;
                    int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    int channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                    int channelConfig = AudioFormat.CHANNEL_OUT_DEFAULT;
                    switch (channelCount) {
                        case 1:
                            channelConfig = AudioFormat.CHANNEL_OUT_MONO;
                            break;
                        case 2:
                            channelConfig = AudioFormat.CHANNEL_OUT_STEREO;
                            break;
                    }
                    int buffSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig,
                            AudioFormat.ENCODING_PCM_16BIT);
                    audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
                            channelConfig, AudioFormat.ENCODING_PCM_16BIT,
                            buffSize, AudioTrack.MODE_STREAM);
                    audioTrack.play();

                    if (audioDecoder.state == Decoder.STATE_UNINITIALIZED) {
                        setupDecoder(audioDecoder, trackSource);
                    }
                    if (audioDecoder.mimeType == null || !audioDecoder.mimeType.equals(trackSource.mimeType)) {
                        if (audioDecoder.state != Decoder.STATE_UNINITIALIZED) {
                            audioDecoder.decoder.release();
                            audioDecoder.state = Decoder.STATE_UNINITIALIZED;
                        }
                        setupDecoder(audioDecoder, trackSource);
                    } else {
                        if (audioDecoder.state == Decoder.STATE_STARTED) {
                            audioDecoder.decoder.stop();
                            audioDecoder.state = Decoder.STATE_INITIALIZED;
                        }
                    }
                    if (audioDecoder.state == Decoder.STATE_INITIALIZED) {
                        audioDecoder.decoder.configure(trackSource.format, null, null, 0);
                        audioDecoder.state = Decoder.STATE_CONFIGURED;
                        audioDecoder.presentationTimeUs = 0;
                    }
                    if (audioDecoder.state == Decoder.STATE_CONFIGURED) {
                        audioDecoder.decoder.start();
                        audioDecoder.state = Decoder.STATE_STARTED;
                    }
                }
                if (audioDecoder.state != Decoder.STATE_STARTED) {
                    throw new RuntimeException("AudioDecoder init failed.");
                }
                SourceState state = stateMap.get(trackSource);
                MediaExtractor extractor = audioExtractors.get(trackSource.source);
                MediaCodec decoder = audioDecoder.decoder;

                if (extractor != null && state != null) {
                    int inputIndex;
                    int inputCount = 0;
//                    do {
                        inputIndex = -1;
                        if (state.timeRangeMs.isTimeInRange(state.positionMs) && !state.ended) {
                            inputIndex = decoder.dequeueInputBuffer(TIMEOUT_US);
                            if (inputIndex >= 0) {
                                ByteBuffer buffer = MediaUtil.getInputBuffer(decoder, inputIndex);
                                int buffSize = extractor.readSampleData(buffer, 0);
                                long time = extractor.getSampleTime();
                                int flags = extractor.getSampleFlags();
//                                Log.d(TAG, "sampleTime: " + time);
                                if (buffSize > 0) {
                                    decoder.queueInputBuffer(inputIndex, 0, buffSize, time, flags);
                                    inputCount += 1;
                                } else {
                                    decoder.queueInputBuffer(inputIndex, 0, 0, 0, 0);
                                    playHandler.sendEmptyMessage(MSG_AUDIO_EOS);
                                }
                            }
                            if (extractor.advance()) {
                                state.positionMs.value = TimeUtil.usToMs(extractor.getSampleTime());
                                state.ended = false;
                            } else {
                                state.ended = true;
                            }
                        }
//                    } while (inputIndex >= 0 || state.ended);
                    boolean decoded = false;
                    while (!decoded) {
                        int outputIndex = decoder.dequeueOutputBuffer(audioDecoder.bufferInfo, TIMEOUT_US);
                        switch (outputIndex) {
                            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                                break;
                            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                                audioDecoder.outputFormat = decoder.getOutputFormat();
                                int sampleRate = audioDecoder.outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                                audioTrack.setPlaybackRate(sampleRate);
                                break;
                            case MediaCodec.INFO_TRY_AGAIN_LATER:
                                decoded = true;
                                break;
                            default:
                                audioDecoder.presentationTimeUs = audioDecoder.bufferInfo.presentationTimeUs;
                                playHandler.obtainMessage(MSG_AUDIO_RENDERED, audioDecoder).sendToTarget();
                                try {
                                    audioSem.acquire();
                                } catch (InterruptedException e) {
                                    return;
                                }
                                ByteBuffer buffer = MediaUtil.getOutputBuffer(decoder, outputIndex);
                                final byte[] chunk = new byte[audioDecoder.bufferInfo.size];
                                buffer.get(chunk);
                                buffer.clear();
                                decoder.releaseOutputBuffer(outputIndex, false);
                                // TODO feed to target
                                if ((audioDecoder.bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                    offsetTime += audioDecoder.presentationTimeUs;
                                    audioDecoder.presentationTimeUs = 0;
                                } else {
                                    audioTrack.write(chunk, 0, audioDecoder.bufferInfo.size);
                                }
                                decoded = true;
                                break;
                        }
                    }

                    if (audioDecoder.presentationTimeUs < state.durationMs.value && !state.ended) {
                        infoView.post(() -> updateInfo(TimeUtil.usToString(audioDecoder.presentationTimeUs + offsetTime) + "\n" +
                                TimeUtil.usToString(playClock.getPositionUs())));
                        Message msg = audioHandler.obtainMessage(MSG_DO_NEXT, trackSource);
                        Log.d(TAG, "presentationTimeUs: " + audioDecoder.presentationTimeUs);

                        if (!audioClock.isStarted()) {
                            audioClock.start();
                            audioHandler.sendMessage(msg);
                        } else {
                            long operationTimeUs = TimeUtil.msToUs(SystemClock.elapsedRealtime()) - operationStartTimeUs;
                            long currentTimeUs = audioClock.getPositionUs();
                            Log.d(TAG, "currentTimeUs: " + currentTimeUs);
                            long interval = audioDecoder.presentationTimeUs - currentTimeUs - operationTimeUs;
//                            Log.d(TAG, "interval: " + interval);
                        if (interval < 0) {
                            audioHandler.sendMessage(msg);
                        } else {
                            audioHandler.sendMessageDelayed(msg, TimeUtil.usToMs(interval));
                        }
//                        audioClock.setPositionUs(audioDecoder.presentationTimeUs);
                        }
                    }
                }
            }
        }
    }

    private void setupDecoder(Decoder decoder, TrackSource trackSource) {
        try {
            decoder.decoder = MediaCodec.createDecoderByType(trackSource.mimeType);
            decoder.state = Decoder.STATE_INITIALIZED;
            decoder.mimeType = trackSource.mimeType;
        } catch (IOException e) {
            e.printStackTrace();
            decoder.decoder = null;
            decoder.state = Decoder.STATE_UNINITIALIZED;
        }
    }

    private class TrackSource {
        String source;
        int trackIndex;
        String mimeType;
        MediaFormat format;

        TrackSource(String source, int trackIndex, String mimeType, MediaFormat format) {
            this.source = source;
            this.trackIndex = trackIndex;
            this.mimeType = mimeType;
            this.format = format;
        }

        @Override
        public String toString() {
            return "TrackSource{" +
                    "source='" + source + '\'' +
                    ", trackIndex=" + trackIndex +
                    ", mimeType='" + mimeType + '\'' +
                    '}';
        }
    }

    private class SourceState {
        TrackSource source;
        Time durationMs;
        TimeRange timeRangeMs;
        Time positionMs;
        boolean ended = false;

        public SourceState(TrackSource source) {
            this.source = source;
            long duration = source.format.getLong(MediaFormat.KEY_DURATION);
            durationMs = new Time(duration);
            positionMs = new Time(0);
            timeRangeMs = new TimeRange(Time.ZERO, durationMs);
        }

        public SourceState(TrackSource source, TimeRange timeRangeMs) {
            this.source = source;
            this.timeRangeMs = timeRangeMs;
            long duration = source.format.getLong(MediaFormat.KEY_DURATION);
            durationMs = new Time(duration);
            positionMs = timeRangeMs.start;
        }
    }

    private class Decoder {
        public static final int STATE_UNINITIALIZED = 0;
        public static final int STATE_INITIALIZED = 1;
        public static final int STATE_CONFIGURED = 2;
        public static final int STATE_STARTED = 3;

        MediaCodec decoder;
        String mimeType;
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        MediaFormat outputFormat;
        long presentationTimeUs = 0;
        int state = STATE_UNINITIALIZED;
    }
}
