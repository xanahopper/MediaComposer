package com.gotokeep.su.composer.util;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.MediaCodec;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * @author xana/cuixianming
 * @version 1.0
 * @since 2018/5/13 14:40
 */
public class MediaUtil {
    public static final String VIDEO_MIME_START = "video/";
    public static final String IMAGE_MIME_START = "image/";

    public static int getRotation(String filePath) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(filePath);
        int rotation = Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));
        retriever.release();
        return rotation;
    }

    public static ByteBuffer getInputBuffer(MediaCodec codec, int inputIndex) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return codec.getInputBuffer(inputIndex);
        } else {
            return codec.getInputBuffers()[inputIndex];
        }
    }

    public static ByteBuffer getOutputBuffer(MediaCodec codec, int inputIndex) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return codec.getOutputBuffer(inputIndex);
        } else {
            return codec.getOutputBuffers()[inputIndex];
        }
    }

    public static String getName(String filePath) {
        String name = Uri.parse(filePath).getLastPathSegment();
        return name != null ? name : UUID.randomUUID().toString().substring(0, 5);
    }

    public static Bitmap flipBitmap(Bitmap origin, boolean recycle) {
        if (origin == null) {
            return null;
        }
        Matrix matrix = new Matrix();
        int width = origin.getWidth();
        int height = origin.getHeight();
        matrix.postScale(1, -1, width / 2, height / 2);
        Bitmap finalImage = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, true);
        if (origin != finalImage && recycle) {
            origin.recycle();
        }
        return finalImage;
    }

    public static float clamp(float value, float low, float high) {
        if (value < low) {
            return low;
        } else if (value > high) {
            return high;
        } else {
            return value;
        }
    }

    public static long getDuration(String filepath) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        if (TextUtils.isEmpty(filepath) || !new File(filepath).exists()) {
            return 0;
        }
        try {
            retriever.setDataSource(filepath);
            String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            retriever.release();
            return TextUtils.isEmpty(duration) ? 0 : Long.parseLong(duration);
        } catch (Exception e) {
            return 0;
        }
    }

    public static int[] getVideoSize(String filePath) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(filePath);
        String width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
        String height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
        retriever.release();
        return new int[]{Integer.valueOf(width), Integer.valueOf(height)};
    }

    public static String getMime(String source) {
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(source));
    }

    public static boolean isImageFile(String path) {
        String mimeType = URLConnection.guessContentTypeFromName(path);
        return mimeType != null && mimeType.startsWith("image");
    }

    public static boolean isVideoFile(String path) {
        String mimeType = URLConnection.guessContentTypeFromName(path);
        return mimeType != null && mimeType.startsWith("video");
    }

    public static String matrixToString(float[] m) {
        if (m == null || m.length != 16) {
            return "";
        } else {
            return String.format("[%2.2f, %2.2f, %2.2f, %2.2f,\n" +
                            " %2.2f, %2.2f, %2.2f, %2.2f,\n" +
                            " %2.2f, %2.2f, %2.2f, %2.2f,\n" +
                            " %2.2f, %2.2f, %2.2f, %2.2f]",
                    m[0], m[4], m[8], m[12],
                    m[1], m[5], m[9], m[13],
                    m[2], m[6], m[10], m[14],
                    m[3], m[7], m[11], m[15]);
        }
    }
}
