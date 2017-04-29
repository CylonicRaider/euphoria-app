package io.euphoria.xkcd.app.impl.connection;

import java.util.HashMap;
import java.util.Map;

import io.euphoria.xkcd.app.connection.Connection;
import io.euphoria.xkcd.app.connection.ConnectionManager;

/** Created by Xyzzy on 2017-02-24. */

/* Implementation of ConnectionManager */
public class ConnectionManagerImpl implements ConnectionManager {

    private final Map<String, ConnectionImpl> connections;

    public ConnectionManagerImpl() {
        connections = new HashMap<>();
    }

    public static ConnectionManager getInstance() {
        return new ConnectionManagerImpl();
    }

    @Override
    public synchronized Connection getConnection(String roomName) {
        return connections.get(roomName);
    }

    @Override
    public synchronized Connection connect(String roomName) {
        ConnectionImpl conn = connections.get(roomName);
        if (conn == null) {
            conn = new ConnectionImpl(this, roomName);
            connections.put(roomName, conn);
        }
        return conn;
    }

    synchronized void remove(ConnectionImpl conn) {
        connections.remove(conn.getRoomName());
    }

    @Override
    public synchronized boolean hasConnections() {
        return !connections.isEmpty();
    }

    @Override
    public synchronized void shutdown() {
        for (ConnectionImpl c : connections.values()) {
            c.close();
        }
    }

}
