package io.euphoria.xkcd.app.connection.event

import io.euphoria.xkcd.app.data.Message

/** Created by Xyzzy on 2017-02-24.  */ /* Event encapsulating a single (potentially fresh) message */
open interface MessageEvent : ConnectionEvent {
    /* The message posted */
    val message: Message
}
