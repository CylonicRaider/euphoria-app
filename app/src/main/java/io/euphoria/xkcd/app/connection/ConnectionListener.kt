package io.euphoria.xkcd.app.connection

import io.euphoria.xkcd.app.connection.event.*

/** Created by Xyzzy on 2017-02-24.  */ /* Receiver interface for connection events */
open interface ConnectionListener {
    /* Connection actually established */
    fun onOpen(evt: OpenEvent?)

    /* Discovered our identity */
    fun onIdentity(evt: IdentityEvent)

    /* Someone (or we) changed their nick */
    fun onNickChange(evt: NickChangeEvent)

    /* Someone (or we) posted a (new) message */
    fun onMessage(evt: MessageEvent)

    /* Someone (or we) joined/left */
    fun onPresenceChange(evt: PresenceChangeEvent)

    /* Backend delivers messages (on demand or not) */
    fun onLogEvent(evt: LogEvent)

    /* The backend (or we) closed the connection */
    fun onClose(evt: CloseEvent)
}
