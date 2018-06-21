package com.gotokeep.su.composer.source;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.webkit.MimeTypeMap;

/**
 * @author xana/cuixianming
 * @version 1.0
 * @since 2018/6/17 17:30
 */
public final class MediaSourceFactory {
    public static MediaSource createMediaSource(Context context, @NonNull Uri sourceUri) {
        MediaSource source = null;
        String type = null;
        if (sourceUri.getScheme().equals(ContentResolver.SCHEME_CONTENT) ||
                sourceUri.getScheme().equals(ContentResolver.SCHEME_ANDROID_RESOURCE)) {
            type = context.getContentResolver().getType(sourceUri);
        } else {
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(sourceUri.toString());
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase());
        }
        if (type != null) {
            if (type.startsWith("video/") || type.startsWith("audio/")) {
                source = new MediaSource(sourceUri.toString());
            }
        }
        return source;
    }
}
