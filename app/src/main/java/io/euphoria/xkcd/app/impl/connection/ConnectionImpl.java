package io.euphoria.xkcd.app.impl.connection;

import android.util.Log;

import java.net.URI;
import java.net.URISyntaxException;
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

public class ConnectionImpl implements Connection {

    private final ConnectionManagerImpl parent;
    private final String roomName;
    private final List<ConnectionListener> listeners;
    private ConnectionStatus status;
    private EuphoriaWebSocketClient client;
    private int seqid;

    public ConnectionImpl(ConnectionManagerImpl parent, String roomName) {
        this.parent = parent;
        this.roomName = roomName;
        this.listeners = new ArrayList<>();
        this.status = ConnectionStatus.CONNECTING;
    }

    public ConnectionManagerImpl getParent() {
        return parent;
    }

    @Override
    public String getRoomName() {
        return roomName;
    }

    public synchronized void connect() {
        try {
            // FIXME: Allow specifying a custom URL template.
            client = new EuphoriaWebSocketClient(this, new URI("wss://euphoria.io/room/" + roomName + "/ws"));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Bad room name (did not form a valid URI)");
        }
        client.connect();
    }

    @Override
    public void close() {
        parent.remove(this);
        client.close();
    }

    protected synchronized int sequence() {
        return seqid++;
    }

    @Override
    public int setNick(String name) {
        return client.sendObject("nick", "name", name);
    }

    @Override
    public int postMessage(String text, String parent) {
        return client.sendObject("send", "content", text, "parent", parent);
    }

    @Override
    public int requestLogs(String before, int count) {
        return client.sendObject("log", "n", count, "before", before);
    }

    @Override
    public synchronized ConnectionStatus getStatus() {
        return status;
    }

    protected void submitEvent(ConnectionEvent evt) {
        List<ConnectionListener> listeners;
        synchronized (this) {
            listeners = new ArrayList<>(this.listeners);
        }
        for (ConnectionListener l : listeners) {
            if (evt instanceof OpenEvent) {
                synchronized (this) {
                    status = ConnectionStatus.CONNECTED;
                }
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
                synchronized (this) {
                    if (((CloseEvent) evt).isFinal()) {
                        status = ConnectionStatus.DISCONNECTED;
                    } else {
                        status = ConnectionStatus.RECONNECTING;
                        connect();
                    }
                }
                l.onClose((CloseEvent) evt);
            } else {
                Log.e("ConnectionImpl", "Unknown connection event " + evt + "; dropping.");
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
