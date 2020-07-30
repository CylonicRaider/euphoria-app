package io.euphoria.xkcd.app.data

/** Created by Xyzzy on 2017-02-24.  */ /* A single Heim message */
interface Message {
    /* The ID of this message as a string */
    val id: String

    /* The parent of this message (null for none) */
    val parent: String?

    /* The timestamp of this message, as milliseconds since Epoch */
    val timestamp: Long

    /* The author of the message */
    val sender: SessionView

    /* The content of the message */
    val content: String

    /* Whether the message was truncated to save bandwidth */
    val isTruncated: Boolean
}
