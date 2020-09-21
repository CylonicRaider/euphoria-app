package io.euphoria.xkcd.app.impl.connection;

import android.os.Handler;
import android.os.Looper;

import java.net.HttpCookie;
import java.util.HashMap;
import java.util.Map;

import io.euphoria.xkcd.app.Settings;
import io.euphoria.xkcd.app.connection.Connection;
import io.euphoria.xkcd.app.connection.ConnectionManager;

/** Created by Xyzzy on 2017-02-24. */

/* Implementation of ConnectionManager */
public class ConnectionManagerImpl implements ConnectionManager {

    private final Settings settings;
    private final Handler handler;
    private final Map<String, ConnectionImpl> connections;

    public ConnectionManagerImpl(Settings settings) {
        this.settings = settings;
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
            conn = new ConnectionImpl(this, roomName, settings.shouldContinuePrevSession() ? settings.getSessionCookie() : null);
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

    @Override
    public void updateSessionCookie(HttpCookie sessionCookie) {
        settings.setSessionCookie(sessionCookie);
    }

    public void invokeLater(Runnable cb) {
        handler.post(cb);
    }

    public void invokeLater(Runnable cb, long delay) {
        handler.postDelayed(cb, delay);
    }

}
