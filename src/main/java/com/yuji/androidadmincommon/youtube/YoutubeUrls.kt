package com.yuji.androidadmincommon.youtube

import android.net.Uri

const val DEFAULT_YOUTUBE_HOME_URL = "https://m.youtube.com/"

fun String.toYoutubeResumeUrl(seconds: Int): String {
    val safeUrl = takeIf { it.isHttpYoutubeUrl() } ?: DEFAULT_YOUTUBE_HOME_URL
    val safeSeconds = seconds.coerceAtLeast(0)
    if (safeSeconds <= 0) return safeUrl

    return runCatching {
        val uri = Uri.parse(safeUrl)
        val builder = uri.buildUpon().clearQuery()
        uri.queryParameterNames
            .filterNot { it == "t" || it == "start" }
            .forEach { name ->
                uri.getQueryParameters(name).forEach { value ->
                    builder.appendQueryParameter(name, value)
                }
            }
        builder.appendQueryParameter("t", "${safeSeconds}s").build().toString()
    }.getOrElse {
        val separator = if (safeUrl.contains("?")) "&" else "?"
        "$safeUrl${separator}t=${safeSeconds}s"
    }
}

fun String.normalizedYoutubeUrl(defaultUrl: String = DEFAULT_YOUTUBE_HOME_URL): String {
    val trimmed = trim()
    if (trimmed.isBlank()) return defaultUrl
    return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        trimmed
    } else {
        "https://$trimmed"
    }
}

fun String.isHttpYoutubeUrl(): Boolean =
    runCatching {
        val uri = Uri.parse(this)
        val scheme = uri.scheme.orEmpty()
        val host = uri.host.orEmpty()
        (scheme == "http" || scheme == "https") &&
            (host.endsWith("youtube.com") || host.endsWith("youtu.be"))
    }.getOrDefault(false)

fun String.isYoutubeVideoUrl(): Boolean =
    contains("youtube.com/watch", ignoreCase = true) ||
        contains("youtu.be/", ignoreCase = true) ||
        contains("youtube.com/shorts/", ignoreCase = true)

