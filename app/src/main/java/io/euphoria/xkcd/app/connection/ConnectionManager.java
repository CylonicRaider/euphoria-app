package io.euphoria.xkcd.app.connection;

import java.net.HttpCookie;

/** Created by Xyzzy on 2017-02-24. */

/* Main entry point of the connection submodule */
public interface ConnectionManager {

    /* Get connection for room name
     *
     * If there is no connection to the room, null is returned. Since that would initiate I/O, lazy creation is not
     * provided.
     */
    Connection getConnection(String roomName);

    /* Connect to a room
     *
     * If a connection is already present, this is equivalent to getConnection().
     */
    Connection connect(String roomName);

    /* Check whether there are still any connections present
     *
     * Immediately after the last connection managed by this ConnectionManager closes, this should return true, so
     * that terminal cleanup can begin.
     */
    boolean hasConnections();

    /* Gracefully shut down the connection manager
     *
     * Any still-present connections should be closed, and any resources held released.
     */
    void shutdown();
}
