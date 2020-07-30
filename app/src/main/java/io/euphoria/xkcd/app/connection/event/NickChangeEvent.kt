package io.euphoria.xkcd.app.connection.event

import io.euphoria.xkcd.app.data.SessionView

/** Created by Xyzzy on 2017-02-24.  */ /* Event encapsulating a nickname change */
open interface NickChangeEvent : ConnectionEvent {
    /* The session affected (after the nickname change) */
    val session: SessionView

    /* The old nickname */
    val oldNick: String?

    /* The new nickname (duplicated from getSession()) */
    val newNick: String?
}
