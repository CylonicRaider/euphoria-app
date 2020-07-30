package io.euphoria.xkcd.app.connection.event

import io.euphoria.xkcd.app.data.Message

/** Created by Xyzzy on 2017-02-24.  */ /* Event encapsulating a batch of old messages */
open interface LogEvent : ConnectionEvent {
    /* The substance of this event */
    val messages: List<Message>
}
