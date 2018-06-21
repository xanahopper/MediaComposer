package com.gotokeep.su.composer.source;

import android.graphics.BitmapFactory;
import android.media.MediaFormat;
import android.net.Uri;

import com.gotokeep.su.composer.time.Time;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * @author xana/cuixianming
 * @version 1.0
 * @since 2018/6/17 22:11
 */
class ImageSource {
    private String filePath;
    private MediaTrack track;

    ImageSource(String filePath, String mimeType) {
        this.filePath = filePath;
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inJustDecodeBounds = true;
        try {
            BitmapFactory.decodeStream(new BufferedInputStream(new FileInputStream(filePath)), null, opt);
            track = new MediaTrack(Uri.parse(filePath), MediaTrack.TYPE_VISUAL, 0, mimeType, Time.INVALID_TIME,
                    MediaFormat.createVideoFormat(mimeType, opt.outWidth, opt.outHeight));
            track.generateSegments();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public int getTrackCount() {
        return 1;
    }

    public MediaTrack getTrack(int index) {
        return track;
    }

    public MediaTrack getTrackWithTrackId(int trackId) {
        return trackId == 0 ? track : null;
    }

    public MediaTrack[] getTracksWithType(int trackType) {
        return trackType == MediaTrack.TYPE_VISUAL ? new MediaTrack[] { track } : new MediaTrack[0];
    }

    public boolean isAvailable() {
        return true;
    }
}
