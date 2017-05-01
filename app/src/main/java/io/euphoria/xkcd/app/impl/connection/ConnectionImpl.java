package io.euphoria.xkcd.app.impl.connection;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import io.euphoria.xkcd.app.connection.Connection;
import io.euphoria.xkcd.app.connection.ConnectionListener;
import io.euphoria.xkcd.app.connection.ConnectionStatus;
import io.euphoria.xkcd.app.connection.event.CloseEvent;
import io.euphoria.xkcd.app.connection.event.ConnectionEvent;
import io.euphoria.xkcd.app.connection.event.IdentityEvent;
import io.euphoria.xkcd.app.connection.event.LogEvent;
import io.euphoria.xkcd.app.connection.event.MessageEvent;
import io.euphoria.xkcd.app.connection.event.NickChangeEvent;
import io.euphoria.xkcd.app.connection.event.OpenEvent;
import io.euphoria.xkcd.app.connection.event.PresenceChangeEvent;

/** Created by Xyzzy on 2017-04-29. */

class ConnectionImpl implements Connection {

    private final ConnectionManagerImpl parent;
    private final String roomName;
    private final List<ConnectionListener> listeners;
    private int seqid;

    public ConnectionImpl(ConnectionManagerImpl parent, String roomName) {
        this.parent = parent;
        this.roomName = roomName;
        listeners = new ArrayList<>();
    }

    public ConnectionManagerImpl getParent() {
        return parent;
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

    protected synchronized int sequence() {
        return seqid++;
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

    protected void submitEvent(ConnectionEvent evt) {
        List<ConnectionListener> listeners;
        synchronized (this) {
            listeners = new ArrayList<>(this.listeners);
        }
        for (ConnectionListener l : listeners) {
            if (evt instanceof OpenEvent) {
                l.onOpen((OpenEvent) evt);
            } else if (evt instanceof IdentityEvent) {
                l.onIdentity((IdentityEvent) evt);
            } else if (evt instanceof NickChangeEvent) {
                l.onNickChange((NickChangeEvent) evt);
            } else if (evt instanceof MessageEvent) {
                l.onMessage((MessageEvent) evt);
            } else if (evt instanceof PresenceChangeEvent) {
                l.onPresenceChange((PresenceChangeEvent) evt);
            } else if (evt instanceof LogEvent) {
                l.onLogEvent((LogEvent) evt);
            } else if (evt instanceof CloseEvent) {
                l.onClose((CloseEvent) evt);
            } else {
                Log.e("ConnectionImpl", "Unknown connection event class; dropping.");
            }
        }
    }

    @Override
    public synchronized void addEventListener(ConnectionListener l) {
        listeners.add(l);
    }

    @Override
    public synchronized void removeEventListener(ConnectionListener l) {
        listeners.remove(l);
    }
}
