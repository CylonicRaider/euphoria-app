package io.euphoria.xkcd.app.impl.connection;

import io.euphoria.xkcd.app.connection.Connection;
import io.euphoria.xkcd.app.connection.ConnectionManager;

/** Created by Xyzzy on 2017-02-24. */

/* Implementation of ConnectionManager */
public class ConnectionManagerImpl implements ConnectionManager {

    @Override
    public Connection getConnection(String roomName) {
        throw new AssertionError("Not implemented");
    }

    @Override
    public Connection connect(String roomName) {
        throw new AssertionError("Not implemented");
    }

}
