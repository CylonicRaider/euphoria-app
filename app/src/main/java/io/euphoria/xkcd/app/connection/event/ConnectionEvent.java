package io.euphoria.xkcd.app.connection.event;

import io.euphoria.xkcd.app.connection.Connection;

/** Created by Xyzzy on 2017-02-24. */

/** A message received by a Connection wrapped into the event concept */
public interface ConnectionEvent {

    /** The connection this event originated from */
    Connection getConnection();

    /** The sequence ID of the request that caused the message, or -1 if none */
    int getSequenceID();

}
