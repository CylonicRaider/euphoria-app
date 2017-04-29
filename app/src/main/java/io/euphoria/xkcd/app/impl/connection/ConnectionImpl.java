package io.euphoria.xkcd.app.impl.connection;

import java.util.ArrayList;
import java.util.List;

import io.euphoria.xkcd.app.connection.Connection;
import io.euphoria.xkcd.app.connection.ConnectionListener;
import io.euphoria.xkcd.app.connection.ConnectionStatus;

/** Created by Xyzzy on 2017-04-29. */

class ConnectionImpl implements Connection {

    private final ConnectionManagerImpl parent;
    private final String roomName;
    private final List<ConnectionListener> listeners;

    public ConnectionImpl(ConnectionManagerImpl parent, String roomName) {
        this.parent = parent;
        this.roomName = roomName;
        listeners = new ArrayList<>();
    }

    @Override
    public String getRoomName() {
        return roomName;
    }

    @Override
    public void close() {
        parent.remove(this);
        throw new AssertionError("Not implemented");
    }

    @Override
    public int setNick(String name) {
        throw new AssertionError("Not implemented");
    }

    @Override
    public int postMessage(String text, String parent) {
        throw new AssertionError("Not implemented");
    }

    @Override
    public int requestLogs(String before, int count) {
        throw new AssertionError("Not implemented");
    }

    @Override
    public ConnectionStatus getStatus() {
        throw new AssertionError("Not implemented");
    }

    @Override
    public void addEventListener(ConnectionListener l) {
        listeners.add(l);
    }

    @Override
    public void removeEventListener(ConnectionListener l) {
        listeners.remove(l);
    }
}
