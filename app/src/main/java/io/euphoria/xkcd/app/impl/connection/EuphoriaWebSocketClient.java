package io.euphoria.xkcd.app.impl.connection;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

import io.euphoria.xkcd.app.connection.Connection;
import io.euphoria.xkcd.app.connection.event.CloseEvent;
import io.euphoria.xkcd.app.connection.event.ConnectionEvent;
import io.euphoria.xkcd.app.connection.event.OpenEvent;

/** Created by Xyzzy on 2017-05-01. */

public class EuphoriaWebSocketClient extends WebSocketClient {

    private class EventImpl implements ConnectionEvent {

        private final int seq;

        public EventImpl(int seq) {
            this.seq = seq;
        }
        public EventImpl() {
            this(-1);
        }

        @Override
        public Connection getConnection() {
            return EuphoriaWebSocketClient.this.parent;
        }

        @Override
        public int getSequenceID() {
            return seq;
        }

    }

    private class OpenEventImpl extends EventImpl implements OpenEvent {}

    private class CloseEventImpl extends EventImpl implements CloseEvent {

        private final boolean fin;

        public CloseEventImpl(boolean fin) {
            this.fin = fin;
        }

        @Override
        public boolean isFinal() {
            return fin;
        }

    }

    private final ConnectionImpl parent;

    public EuphoriaWebSocketClient(ConnectionImpl parent, URI endpoint) {
        super(endpoint);
        this.parent = parent;
    }

    public ConnectionImpl getParent() {
        return parent;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        parent.submitEvent(new OpenEventImpl());
    }

    @Override
    public void onMessage(String message) {
        throw new AssertionError("Not implemented");
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        parent.submitEvent(new CloseEventImpl(true));
    }

    @Override
    public void onError(Exception ex) {
        parent.submitEvent(new CloseEventImpl(false));
    }

}
