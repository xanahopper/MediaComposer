package com.gotokeep.su.composer;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.gotokeep.su.composer.composition.Composition;
import com.gotokeep.su.composer.composition.CompositionTrack;
import com.gotokeep.su.composer.render.AudioRenderResult;
import com.gotokeep.su.composer.render.VideoRenderNode;
import com.gotokeep.su.composer.render.VideoRenderResult;
import com.gotokeep.su.composer.source.MediaSource;
import com.gotokeep.su.composer.source.MediaSourceFactory;
import com.gotokeep.su.composer.source.MediaTrack;
import com.gotokeep.su.composer.time.Time;
import com.gotokeep.su.composer.time.TimeRange;
import com.gotokeep.su.composer.timeline.SourceItem;
import com.gotokeep.su.composer.timeline.Timeline;
import com.gotokeep.su.composer.timeline.TimelineItem;
import com.gotokeep.su.composer.timeline.TimelineTrack;
import com.gotokeep.su.composer.util.Size;
import com.gotokeep.su.composer.util.TimeUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author xana/cuixianming
 * @version 1.0
 * @since 2018/6/17 16:27
 */
class MediaComposerImpl implements MediaComposer, Handler.Callback, ComposerOutput.OnAvailableListener {
    private static final int MSG_SET_TIMELINE = 933;
    private static final int MSG_SET_TARGET = 609;
    private static final int MSG_START = 784;
    private static final int MSG_STOP = 670;
    private static final int MSG_RELEASE = 937;
    private static final int MSG_SEEK = 674;
    private static final int MSG_SET_LOOP = 661;
    private static final int MSG_DO_WORK = 493;

    public static final int MSG_VIDEO_READY = 0;
    public static final int MSG_AUDIO_READY = 1;

    private Timeline timeline;
    private ComposerOutput composerTarget;
    private ComposerSynchronizer composerSynchronizer;
    private Composition composition;
    private TimelineTrack sourceTrack;
    private TimelineTrack audioTrack;
    private Map<Uri, MediaTrack> videoTracks = new HashMap<>();
    private Map<Uri, MediaTrack> audioTracks = new HashMap<>();

    private LinkedBlockingQueue<AudioRenderResult> audioRenderResults = new LinkedBlockingQueue<>();
    private VideoRenderResult videoRenderResult = null;

    private EventDispatcher eventDispatcher;

    private HandlerThread internalThread;
    private Handler handler;

    private AtomicBoolean ready = new AtomicBoolean(false);
    private AtomicBoolean started = new AtomicBoolean(false);
    private boolean loop = false;
    private Context context;
    private final Object releaseSynObj = new Object();
    private boolean released = false;

    MediaComposerImpl(Context context) {
        this.context = context;
        internalThread = new HandlerThread("ComposerImpl");
        internalThread.start();
        handler = new Handler(internalThread.getLooper(), this);

        composerSynchronizer = new ComposerSynchronizer(context, handler);
    }

    @Override
    public void setTimeline(Timeline timeline) {
        handler.obtainMessage(MSG_SET_TIMELINE, timeline).sendToTarget();
    }

    @Override
    public void setTarget(ComposerOutput target) {
        handler.obtainMessage(MSG_SET_TARGET, target).sendToTarget();
    }

    @Override
    public void start() {
        handler.sendEmptyMessage(MSG_START);
    }

    @Override
    public void stop() {
        handler.sendEmptyMessage(MSG_STOP);
    }

    @Override
    public void seekTo(long seekTimeMs) {
        handler.obtainMessage(MSG_SEEK, new Time(TimeUtil.msToUs(seekTimeMs))).sendToTarget();
    }

    @Override
    public void setLoop(boolean loop) {
        handler.obtainMessage(MSG_SET_LOOP, loop ? 1 : 0, 0);
    }

    @Override
    public void addEventListener(EventListener listener) {
        eventDispatcher.addEventListener(listener);
    }

    @Override
    public void removeEventListener(EventListener listener) {
        eventDispatcher.removeEventListener(listener);
    }

    @Override
    public void release() {
        synchronized (releaseSynObj) {
            handler.sendEmptyMessage(MSG_RELEASE);
            try {
                releaseSynObj.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        if (!ready.get() && msg.what != MSG_SET_TARGET) {
            handler.sendMessageDelayed(msg, 10);
            return false;
        }
        switch (msg.what) {
            case MSG_SET_TARGET:
                setTargetInternal((ComposerOutput) msg.obj);
                break;
            case MSG_SET_TIMELINE:
                updateSources(this.timeline, (Timeline) msg.obj);
                break;
            case MSG_SEEK:
                composerSynchronizer.seekTo((Time) msg.obj);
                break;
            case MSG_START:
                started.set(true);
                handler.sendEmptyMessage(MSG_DO_WORK);
                break;
            case MSG_STOP:
                started.set(false);
                composerSynchronizer.stop();
                break;
            case MSG_AUDIO_READY:
                try {
                    audioRenderResults.put((AudioRenderResult) msg.obj);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case MSG_VIDEO_READY:
                if (videoRenderResult != null) {
                    videoRenderResult.recycle();
                }
                videoRenderResult = (VideoRenderResult) msg.obj;
                break;
            case MSG_DO_WORK:
                doComposeWork();
                break;

        }
        return false;
    }

    private void doComposeWork() {
        // 取出音视频解码素材
        if (started.get()) {
            /// 此处同步由 ComposerSynchronizer 完成，播放模式下音频优先，视频同步音频，
            /// 导出模式下，在同步的基础上，音视频数据必须严格对应才会被推送至此，保证写入正确
            if (!audioRenderResults.isEmpty()) {
                AudioRenderResult audioResult = audioRenderResults.poll();
                composerTarget.writeChunkData(audioResult.chunk, audioResult.bufferSize, audioResult.presentationTimeUs,
                        audioResult.flags);
                audioResult.recycle();
            }
            if (videoRenderResult != null) {
                composerTarget.renderFrame(videoRenderResult.outputTexture, videoRenderResult.presentationTimeUs,
                        videoRenderResult.flags);
                videoRenderResult.recycle();
                videoRenderResult = null;
            }
            handler.sendEmptyMessage(MSG_DO_WORK);
        }
    }

    private void setTargetInternal(ComposerOutput target) {
        if (composerTarget != target) {
            if (this.composerTarget != null) {
                this.composerTarget.release();
            }
            this.composerTarget = target;

            if (target != null) {
                if (target.isAvailable()) {
                    // init DecodeEngine and RenderEngine
                    composerSynchronizer.configure(target.getTargetSurface(), target.getTargetSize());
                    ready.set(true);
                    target.setAvailableListener(null);
                } else {
                    target.setAvailableListener(this);
                }
            } else {
                composerSynchronizer.configure(null, new Size(1, 1));
            }
        }
    }

    private void updateSources(Timeline oldTimeline, Timeline newTimeline) {
        if (!started.get()) {
            stop();
            setTimeline(newTimeline);
            return;
        }
        this.timeline = newTimeline;
        eventDispatcher.onTimelineChanged(this, newTimeline);

        if (oldTimeline != newTimeline) {
            eventDispatcher.onPreprocess(this);
            clearTracks();
            if (newTimeline != null) {
                this.sourceTrack = newTimeline.getSourceTrack();
                this.audioTrack = newTimeline.getAudioTrack();

                for (TimelineItem item : sourceTrack.getItems()) {
                    MediaSource mediaSource = MediaSourceFactory.createMediaSource(context, item.getSource(0));
                    for (MediaTrack track : mediaSource.getTracksWithType(MediaTrack.TYPE_VISUAL)) {
                        videoTracks.put(track.getSource(), track);
                    }
                    for (MediaTrack track : mediaSource.getTracksWithType(MediaTrack.TYPE_AUDIBLE)) {
                        audioTracks.put(track.getSource(), track);
                    }
                }
                if (audioTrack != null) {
                    for (TimelineItem item : audioTrack.getItems()) {
                        MediaSource mediaSource = MediaSourceFactory.createMediaSource(context, item.getSource(0));
                        for (MediaTrack track : mediaSource.getTracksWithType(MediaTrack.TYPE_AUDIBLE)) {
                            audioTracks.put(track.getSource(), track);
                        }
                    }
                }
                this.composition = generateComposition(newTimeline);

                composerSynchronizer.flush();
                composerSynchronizer.setExportMode(composerTarget.isExport());
                composerSynchronizer.setComposition(composition);
                if (composition != null) {
                   seekToInternal(composition.getTimeRange().start);
                }
            }
        }
    }

    private void clearTracks() {
        videoTracks.clear();
        audioTracks.clear();
    }

    private void seekToInternal(Time time) {
        composerSynchronizer.prepareVideo(composition.getTracksWithTypeAtTime(MediaTrack.TYPE_VISUAL, time));
        composerSynchronizer.prepareAudio(composition.getTracksWithTypeAtTime(MediaTrack.TYPE_AUDIBLE, time));
    }

    private Composition generateComposition(Timeline timeline) {
        Composition composition = new Composition();
        CompositionTrack videoTrack = composition.addTrack(MediaTrack.TYPE_VISUAL);
        CompositionTrack audioTrack = composition.addTrack(MediaTrack.TYPE_AUDIBLE);
        boolean originAudio = (timeline.getAudioTrack() == null);

        for (int i = 0; i < timeline.getTrackCount(); i++) {
            TimelineTrack track = timeline.getTrack(i);
            List<TimelineItem> items = track.getItems();
            switch (track.getTrackType()) {
                case TimelineTrack.TRACK_TYPE_SOURCE:
                    for (TimelineItem item : items) {
                        Uri sourceUri = item.getSource(0);
                        MediaTrack videoSourceTrack = videoTracks.get(sourceUri);
                        if (videoSourceTrack != null) {
                            videoTrack.addSegment(videoSourceTrack, item.getTimeRange());
                        }
                        if (originAudio && audioTracks.containsKey(sourceUri)) {
                            MediaTrack audioSourceTrack = audioTracks.get(sourceUri);
                            audioTrack.addSegment(audioSourceTrack, item.getTimeRange());
                        }
                    }
                    break;
                case TimelineTrack.TRACK_TYPE_TRANSITION:
                    for (TimelineItem item : items) {
                        Uri startUri = item.getSource(0);
                        Uri endUri = item.getSource(1);

                    }
                    break;
                case TimelineTrack.TRACK_TYPE_FILTER:
                    break;
                case TimelineTrack.TRACK_TYPE_LAYER:
                    break;
            }
        }
        return composition;
    }

    @Override
    public void onTargetAvailable(ComposerOutput target) {
        handler.obtainMessage(MSG_SET_TARGET, target).sendToTarget();
    }

    @Override
    public void onTargetUnavailable() {
        handler.obtainMessage(MSG_SET_TARGET, null).sendToTarget();
    }

    private class EventDispatcher implements EventListener {
        private List<WeakReference<EventListener>> eventListenerRefs = new ArrayList<>();

        public void addEventListener(EventListener listener) {
            eventListenerRefs.add(new WeakReference<>(listener));
        }

        public void removeEventListener(EventListener listener) {
            Iterator<WeakReference<EventListener>> iterator = eventListenerRefs.iterator();
            while (iterator.hasNext()) {
                WeakReference<EventListener> ref = iterator.next();
                if (ref.get() == null || ref.get() == listener) {
                    iterator.remove();
                }
            }
        }

        @Override
        public void onTimelineChanged(MediaComposer composer, Timeline timeline) {
            for (WeakReference<EventListener> ref : eventListenerRefs) {
                if (ref != null && ref.get() != null) {
                    ref.get().onTimelineChanged(composer, timeline);
                }
            }
        }

        @Override
        public void onPreprocess(MediaComposer composer) {
            for (WeakReference<EventListener> ref : eventListenerRefs) {
                if (ref != null && ref.get() != null) {
                    ref.get().onPreprocess(composer);
                }
            }
        }

        @Override
        public void onStart(MediaComposer composer) {
            for (WeakReference<EventListener> ref : eventListenerRefs) {
                if (ref != null && ref.get() != null) {
                    ref.get().onStart(composer);
                }
            }
        }

        @Override
        public void onPause(MediaComposer composer) {
            for (WeakReference<EventListener> ref : eventListenerRefs) {
                if (ref != null && ref.get() != null) {
                    ref.get().onPause(composer);
                }
            }
        }

        @Override
        public void onStop(MediaComposer composer) {
            for (WeakReference<EventListener> ref : eventListenerRefs) {
                if (ref != null && ref.get() != null) {
                    ref.get().onStop(composer);
                }
            }
        }

        @Override
        public void onPositionChange(MediaComposer composer, long currentTime, TimeRange totalTime) {
            for (WeakReference<EventListener> ref : eventListenerRefs) {
                if (ref != null && ref.get() != null) {
                    ref.get().onPositionChange(composer, currentTime, totalTime);
                }
            }
        }

        @Override
        public void onError(MediaComposer composer, Exception exception) {
            for (WeakReference<EventListener> ref : eventListenerRefs) {
                if (ref != null && ref.get() != null) {
                    ref.get().onError(composer, exception);
                }
            }
        }
    }
}
