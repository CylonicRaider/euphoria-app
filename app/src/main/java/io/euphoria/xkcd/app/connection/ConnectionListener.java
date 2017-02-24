package io.euphoria.xkcd.app.connection;

import io.euphoria.xkcd.app.connection.event.LogEvent;
import io.euphoria.xkcd.app.connection.event.MessageEvent;
import io.euphoria.xkcd.app.connection.event.NickChangeEvent;
import io.euphoria.xkcd.app.connection.event.PresenceChangeEvent;

/** Created by Xyzzy on 2017-02-24. */

/** Receiver interface for connection events */
public interface ConnectionListener {

    /** Someone (or we) changed their nick */
    void onNickChange(NickChangeEvent evt);

    /** Someone (or we) posted a (new) message */
    void onMessage(MessageEvent evt);

    /** Someone (or we) joined/left */
    void onPresenceChange(PresenceChangeEvent evt);

    /** Backend delivers messages (on demand or not) */
    void onLogEvent(LogEvent evt);

}
