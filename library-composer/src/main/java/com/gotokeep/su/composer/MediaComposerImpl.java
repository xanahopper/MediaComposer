package com.gotokeep.su.composer;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.view.Surface;

import com.gotokeep.su.composer.composition.Composition;
import com.gotokeep.su.composer.source.MediaTrack;
import com.gotokeep.su.composer.time.TimeRange;
import com.gotokeep.su.composer.timeline.Timeline;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author xana/cuixianming
 * @version 1.0
 * @since 2018/6/17 16:27
 */
class MediaComposerImpl implements MediaComposer, Handler.Callback, ComposerTarget.RenderTargetAvailableListener {
    private Timeline timeline;
    private ComposerTarget composerTarget;

    private EventDispatcher eventDispatcher;

    private HandlerThread internalThread;
    private Handler handler;

    private boolean loop = false;
    private Composition composition = null;

    MediaComposerImpl() {
        internalThread = new HandlerThread("ComposerImpl");
        internalThread.start();
        handler = new Handler(internalThread.getLooper(), this);
    }

    @Override
    public void setTimeline(Timeline timeline) {
        updateSources(this.timeline, timeline);
    }

    @Override
    public void setTarget(ComposerTarget target) {
//        if (this.renderTarget != null) {
//            this.renderTarget.release();
//        }
//        this.renderTarget = renderTarget;
//        if (renderTarget != null) {
//            renderTarget.setAvailableListener(this);
//        }
    }

    @Override
    public void start() {

    }

    @Override
    public void pause() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void seekTo(long seekTimeMs) {

    }

    @Override
    public void setLoop(boolean loop) {

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

    }

    @Override
    public boolean handleMessage(Message msg) {
        return false;
    }

    @Override
    public void onTargetSurfaceAvailable(Surface targetSurface) {

    }

    private void updateSources(Timeline oldTimeline, Timeline newTimeline) {

    }

    private void sendRenderRequest() {
        if (composition != null && composition.isAvailable()) {
            for (int i = 0; i < composition.getTrackCount(); i++) {
                MediaTrack track = composition.getTrack(i);

            }
        }
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
