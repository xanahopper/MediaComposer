package com.gotokeep.su.composer.render;

import java.nio.ByteBuffer;

/**
 * Project:  MediaComposer
 * Author:   Xana Hopper(xanahopper@163.com)
 * Created:  2018/6/23 15:01
 */
public interface AudioOutput {
    int writeChunkData(ByteBuffer buffer, int bufferSize, long presentationTimeUs, int flags);

    int writeChunkData(byte[] chunk, int bufferSize, long presentationTimeUs, int flags);
}
