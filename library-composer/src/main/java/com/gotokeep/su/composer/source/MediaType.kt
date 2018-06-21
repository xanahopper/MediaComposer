package com.gotokeep.su.composer.source

/**
 * @author  xana/cuixianming
 * @since   2018/6/18 21:45
 * @version 1.0
 */
enum class MediaType(val type: String, val mimeStart: String) {
    VIDEO("video", "video/"),
    AUDIO("audio", "audio/"),
    TEXT("text", "text/"),
    SUBTITLE("subtitle", ""),
    MUXED("muxed", "")
}