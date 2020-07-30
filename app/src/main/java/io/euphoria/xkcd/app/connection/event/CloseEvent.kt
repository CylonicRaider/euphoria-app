package io.euphoria.xkcd.app.connection.event

/** Created by Xyzzy on 2017-03-05.  */ /* An event informing about the connection having been closed */
open interface CloseEvent : ConnectionEvent {
    /* Whether the close was initiated by user action or by network interference
     *
     * If true, the close stemmed from the user, and the connection will not be re-established; otherwise, an
     * automated re-connection attempt will take place.
     */
    val isFinal: Boolean
}
