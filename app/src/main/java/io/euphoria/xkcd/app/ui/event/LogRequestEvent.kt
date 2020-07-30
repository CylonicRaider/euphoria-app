package io.euphoria.xkcd.app.ui.event

/** Created by Xyzzy on 2017-02-26.  */ /* Event encapsulating the desire to see more room logs */
interface LogRequestEvent : UIEvent {
    /* The message ID down to which to request logs */
    val before: String?
}
