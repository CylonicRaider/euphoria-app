package io.euphoria.xkcd.app.impl.connection;

import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

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

    public int sendObject(String type, Object... data) {
        if (data.length == 0) data = null;
        int seq = parent.sequence();
        try {
            send(buildJSONObject("type", type, "id", seq, "data", buildJSONObject(data)).toString());
        } catch (JSONException exc) {
            Log.e("EuphoriaWebSocketClient", "Exception while serializing JSON", exc);
        }
        return seq;
    }

    private static JSONObject buildJSONObject(Object... data) throws JSONException {
        if (data.length % 2 != 0) throw new IllegalArgumentException("Invalid JSON object construction shortcut");
        JSONObject ret = new JSONObject();
        for (int i = 0; i < data.length; i += 2) {
            if (! (data[i] instanceof String)) throw new JSONException("Object key is not a string");
            if (data[i + 1] == null) continue;
            ret.put((String) data[i], data[i + 1]);
        }
        return ret;
    }

}
