package dev.nemeyes.nextvideo.nextcloud.webdav

/** Thrown when a WebDAV/HTTP call returns a non-success status (no body is parsed). */
class WebDavHttpException(
    val httpCode: Int,
) : Exception("WebDAV request failed with HTTP $httpCode")
