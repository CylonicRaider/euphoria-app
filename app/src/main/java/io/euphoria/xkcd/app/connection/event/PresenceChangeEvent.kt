package io.euphoria.xkcd.app.connection.event

import io.euphoria.xkcd.app.data.SessionView

/** Created by Xyzzy on 2017-02-24.  */ /* Event encapsulating a presence change */
open interface PresenceChangeEvent : ConnectionEvent {
    /* The presence changes announced */
    val sessions: List<SessionView>

    /* Whether the sessions joined or left */
    val isPresent: Boolean
}
