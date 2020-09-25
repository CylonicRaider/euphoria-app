package io.euphoria.xkcd.app.impl.connection;

import android.os.Handler;
import android.os.Looper;

import java.util.HashMap;
import java.util.Map;

import io.euphoria.xkcd.app.connection.Connection;
import io.euphoria.xkcd.app.connection.ConnectionManager;
import io.euphoria.xkcd.app.connection.SessionCookieStore;

/** Created by Xyzzy on 2017-02-24. */

/* Implementation of ConnectionManager */
public class ConnectionManagerImpl implements ConnectionManager {

    final SessionCookieStore sessionCookieStore;
    private final Handler handler;
    private final Map<String, ConnectionImpl> connections;

    public ConnectionManagerImpl(SessionCookieStore sessionCookieStore) {
        this.sessionCookieStore = sessionCookieStore;
        handler = new Handler(Looper.getMainLooper());
        connections = new HashMap<>();
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
            conn.connect();
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

    public void invokeLater(Runnable cb) {
        handler.post(cb);
    }

    public void invokeLater(Runnable cb, long delay) {
        handler.postDelayed(cb, delay);
    }

}
