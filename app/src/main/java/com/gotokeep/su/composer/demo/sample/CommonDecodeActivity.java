package com.gotokeep.su.composer.demo.sample;

import android.graphics.SurfaceTexture;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.opengl.EGLSurface;
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
import com.gotokeep.su.composer.gles.EglCore;
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

/**
 * @author xana/cuixianming
 * @version 1.0
 * @since 2018-06-22 18:15
 */
public class CommonDecodeActivity extends SampleActivity implements Handler.Callback, TextureView.SurfaceTextureListener {
    private static final String TAG = CommonDecodeActivity.class.getSimpleName();
    public static final int TIMEOUT_US = 1000;
    private MediaClock playClock = new MediaClock();
    private MediaClock audioClock = new MediaClock();
    private Decoder videoDecoder = new Decoder();
    private Decoder audioDecoder = new Decoder();

    private Map<String, MediaExtractor> videoExtractors = new HashMap<>();
    private Map<String, MediaExtractor> audioExtractors = new HashMap<>();
    private List<TrackSource> videoTrackSources = new ArrayList<>();
    private List<TrackSource> audioTrackSources = new ArrayList<>();

    private Map<TrackSource, SourceState> stateMap = new HashMap<>();

    private EglCore eglCore;
    private EGLSurface eglSurface;
    private Surface outputSurface;

    private HandlerThread videoThread;
    private Handler videoHandler;

    private HandlerThread audioThread;
    private AudioHandler audioHandler;

    private HandlerThread playThread;
    private Handler playHandler;
    private PlayHandlerCallback playCallback = new PlayHandlerCallback();

    private static final int MSG_SETUP = 861;
    private static final int MSG_SET_SURFACE = 879;
    private static final int MSG_PREPARE = 154;
    private static final int MSG_PLAY = 706;
    private static final int MSG_DO_NEXT = 722;
    private static final int MSG_VIDEO_RENDERED = 831;
    private static final int MSG_AUDIO_RENDERED = 757;

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
        videoHandler = new Handler(videoThread.getLooper(), this);

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
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_SETUP:
                setup();
                break;
            case MSG_SET_SURFACE:
                setSurfaceInternal((Surface) msg.obj);
                break;
        }
        return false;
    }

    private void setup() {
        eglCore = new EglCore();
        if (previewView.isAvailable()) {
            videoHandler.obtainMessage(MSG_SET_SURFACE, new Surface(previewView.getSurfaceTexture())).sendToTarget();
        } else {
            videoHandler.obtainMessage(MSG_SET_SURFACE, null).sendToTarget();
        }
    }

    private void setSurfaceInternal(Surface surface) {
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
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        videoHandler.obtainMessage(MSG_SET_SURFACE, new Surface(surface)).sendToTarget();
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
            }
            return false;
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
                        audioTrackSource = new TrackSource(source, audioIndex, audioMime, videoExtractor.getTrackFormat(audioIndex));
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
                    int buffSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO,
                            AudioFormat.ENCODING_PCM_16BIT);
                    audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
                            AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
                            buffSize, AudioTrack.MODE_STREAM);
                    audioTrack.play();

                    if (audioDecoder.state == Decoder.STATE_UNINITIALIZED) {
                        setupDecoder(trackSource);
                    }
                    if (audioDecoder.mimeType == null || !audioDecoder.mimeType.equals(trackSource.mimeType)) {
                        if (audioDecoder.state != Decoder.STATE_UNINITIALIZED) {
                            audioDecoder.decoder.release();
                            audioDecoder.state = Decoder.STATE_UNINITIALIZED;
                        }
                        setupDecoder(trackSource);
                    }
                    if (audioDecoder.state == Decoder.STATE_INITIALIZED) {
                        audioDecoder.decoder.configure(trackSource.format, null, null, 0);
                        audioDecoder.state = Decoder.STATE_CONFIGURED;
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
                    do {
                        inputIndex = -1;
                        if (state.timeRangeMs.isTimeInRange(state.positionMs) && !state.ended) {
                            inputIndex = decoder.dequeueInputBuffer(TIMEOUT_US);
                            if (inputIndex >= 0) {
                                ByteBuffer buffer = MediaUtil.getInputBuffer(decoder, inputIndex);
                                int buffSize = extractor.readSampleData(buffer, 0);
                                long time = extractor.getSampleTime();
                                int flags = extractor.getSampleFlags();
                                Log.d(TAG, "sampleTime: " + time);
                                if (buffSize > 0) {
                                    decoder.queueInputBuffer(inputIndex, 0, buffSize, time, flags);
                                    inputCount += 1;
                                } else {
                                    throw new RuntimeException("Audio Range error");
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
                    Log.d(TAG, "feed input count: " + inputCount);
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
                                ByteBuffer buffer = MediaUtil.getOutputBuffer(decoder, outputIndex);
                                final byte[] chunk = new byte[audioDecoder.bufferInfo.size];
                                buffer.get(chunk);
                                buffer.clear();
                                decoder.releaseOutputBuffer(outputIndex, false);
                                long time = SystemClock.elapsedRealtime();
                                audioTrack.write(chunk, 0, audioDecoder.bufferInfo.size);
                                Log.d(TAG, "Audio write time: " + (SystemClock.elapsedRealtime() - time));
                                decoded = true;
                                break;
                        }
                    }

                    if (audioDecoder.presentationTimeUs < state.durationMs.value) {
                        infoView.post(() -> updateInfo(TimeUtil.usToString(audioDecoder.presentationTimeUs) + "\n" + TimeUtil.usToString(playClock.getPositionUs())));
                        Message msg = audioHandler.obtainMessage(MSG_DO_NEXT, trackSource);

                        if (!audioClock.isStarted()) {
                            audioClock.start();
                            audioHandler.sendMessage(msg);
                        } else {
                            long operationTimeUs = TimeUtil.msToUs(SystemClock.elapsedRealtime()) - operationStartTimeUs;
                            long currentTimeUs = audioClock.getPositionUs();
                            Log.d(TAG, "currentTimeUs: " + currentTimeUs);
                            Log.d(TAG, "presentationTimeUs: " + audioDecoder.presentationTimeUs);
                            long interval = audioDecoder.presentationTimeUs - currentTimeUs - operationTimeUs;
                            Log.d(TAG, "interval: " + interval);
//                        if (interval < 0) {
                            audioHandler.sendMessage(msg);
//                        } else {
//                            audioHandler.sendMessageDelayed(msg, TimeUtil.usToMs(interval));
//                        }
//                        audioClock.setPositionUs(audioDecoder.presentationTimeUs);
                        }
                    }
                }
            }
        }
    }

    private void setupDecoder(TrackSource trackSource) {
        try {
            audioDecoder.decoder = MediaCodec.createDecoderByType(trackSource.mimeType);
            audioDecoder.state = Decoder.STATE_INITIALIZED;
            audioDecoder.mimeType = trackSource.mimeType;
        } catch (IOException e) {
            e.printStackTrace();
            audioDecoder.decoder = null;
            audioDecoder.state = Decoder.STATE_UNINITIALIZED;
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
