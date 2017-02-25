package io.euphoria.xkcd.app.data;

/* Created by Xyzzy on 2017-02-24. */

/* A single Heim message */
public interface Message {

    /* The ID of this message as a string */
    String getID();

    /* The parent of this message (null for none) */
    String getParent();

    /* The timestamp of this message, as milliseconds since Epoch */
    long getTimestamp();

    /* The author of the message */
    SessionView getSender();

    /* The content of the message */
    String getContent();

    /* Whether the message was truncated to save bandwidth */
    boolean isTruncated();

}
