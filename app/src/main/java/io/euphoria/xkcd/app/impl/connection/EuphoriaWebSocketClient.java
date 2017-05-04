package io.euphoria.xkcd.app.impl.connection;

import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.euphoria.xkcd.app.connection.Connection;
import io.euphoria.xkcd.app.connection.event.CloseEvent;
import io.euphoria.xkcd.app.connection.event.ConnectionEvent;
import io.euphoria.xkcd.app.connection.event.IdentityEvent;
import io.euphoria.xkcd.app.connection.event.LogEvent;
import io.euphoria.xkcd.app.connection.event.MessageEvent;
import io.euphoria.xkcd.app.connection.event.NickChangeEvent;
import io.euphoria.xkcd.app.connection.event.OpenEvent;
import io.euphoria.xkcd.app.connection.event.PresenceChangeEvent;
import io.euphoria.xkcd.app.data.Message;
import io.euphoria.xkcd.app.data.SessionView;

/** Created by Xyzzy on 2017-05-01. */

public class EuphoriaWebSocketClient extends WebSocketClient {

    private static class SessionViewImpl implements SessionView {

        private final String sessionID;
        private final String agentID;
        private final String name;
        private final boolean staff;
        private final boolean manager;

        public SessionViewImpl(String sessionID, String agentID, String name, boolean staff, boolean manager) {
            this.sessionID = sessionID;
            this.agentID = agentID;
            this.name = name;
            this.staff = staff;
            this.manager = manager;
        }

        public SessionViewImpl(SessionView base, String newName) {
            this(base.getSessionID(), base.getAgentID(), newName, base.isStaff(), base.isManager());
        }

        @Override
        public String getSessionID() {
            return sessionID;
        }

        @Override
        public String getAgentID() {
            return agentID;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean isStaff() {
            return staff;
        }

        @Override
        public boolean isManager() {
            return manager;
        }

    }

    private static class MessageImpl implements Message {

        private final String id;
        private final String parent;
        private final long timestamp;
        private final SessionView sender;
        private final String content;
        private final boolean truncated;

        private MessageImpl(String id, String parent, long timestamp, SessionView sender, String content,
                            boolean truncated) {
            this.id = id;
            this.parent = parent;
            this.timestamp = timestamp;
            this.sender = sender;
            this.content = content;
            this.truncated = truncated;
        }

        @Override
        public String getID() {
            return id;
        }

        @Override
        public String getParent() {
            return parent;
        }

        @Override
        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public SessionView getSender() {
            return sender;
        }

        @Override
        public String getContent() {
            return content;
        }

        @Override
        public boolean isTruncated() {
            return truncated;
        }

    }

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

    private class IdentityEventImpl extends EventImpl implements IdentityEvent {

        private final SessionView session;

        public IdentityEventImpl(SessionView session) {
            this.session = session;
        }

        @Override
        public SessionView getIdentity() {
            return session;
        }

    }

    private class NickChangeEventImpl extends EventImpl implements NickChangeEvent {

        private final SessionView session;
        private final String oldNick;

        private NickChangeEventImpl(SessionView session, String oldNick) {
            this.session = session;
            this.oldNick = oldNick;
        }

        @Override
        public SessionView getSession() {
            return session;
        }

        @Override
        public String getOldNick() {
            return oldNick;
        }

        @Override
        public String getNewNick() {
            return session.getName();
        }

    }

    private class MessageEventImpl extends EventImpl implements MessageEvent {

        private final Message message;

        private MessageEventImpl(Message message) {
            this.message = message;
        }

        @Override
        public Message getMessage() {
            return message;
        }

    }

    private class PresenceChangeEventImpl extends EventImpl implements PresenceChangeEvent {

        private final List<SessionView> sessions;

         private final boolean present;

         private PresenceChangeEventImpl(List<SessionView> sessions, boolean present) {
             this.sessions = sessions;
             this.present = present;
         }

         @Override
         public List<SessionView> getSessions() {
             return sessions;
         }

         @Override
         public boolean isPresent() {
             return present;
         }

     }

    private class LogEventImpl extends EventImpl implements LogEvent {

        private final List<Message> messages;

        private LogEventImpl(List<Message> messages) {
            this.messages = messages;
        }

        @Override
        public List<Message> getMessages() {
            return messages;
        }

    }

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
    private final Map<String, SessionView> sessions;

    public EuphoriaWebSocketClient(ConnectionImpl parent, URI endpoint) {
        super(endpoint);
        this.parent = parent;
        this.sessions = new HashMap<>();
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
        JSONObject pmessage;
        try {
            pmessage = new JSONObject(message);
        } catch (JSONException e) {
            Log.e("EuphoriaWebSocketClient", "Received malformed JSON", e);
            return;
        }
        String type;
        JSONObject data;
        try {
            type = pmessage.getString("type");
            data = pmessage.optJSONObject("data");
        } catch (JSONException e) {
            Log.e("EuphoriaWebSocketClient", "Server packet did not contain type", e);
            return;
        }
        try {
            switch (type) {
                case "hello-event":
                    submitEvent(new IdentityEventImpl(parseSessionView(data.getJSONObject("session"))));
                    break;
                case "join-event":
                    submitEvent(new PresenceChangeEventImpl(Collections.singletonList(parseSessionView(data)),
                            true));
                    break;
                // network-event is NYI
                case "nick-event": case "nick-reply":
                    SessionView session = sessions.get(data.getString("session_id"));
                    submitEvent(new NickChangeEventImpl(new SessionViewImpl(session, data.getString("to")),
                            session.getName()));
                    break;
                case "part-event":
                    submitEvent(new PresenceChangeEventImpl(Collections.singletonList(parseSessionView(data)),
                            false));
                    break;
                case "send-event": case "send-reply":
                    parent.submitEvent(new MessageEventImpl(parseMessage(data)));
                    break;
                case "snapshot-event":
                    submitEvent(new PresenceChangeEventImpl(parseSessionViewArray(data.getJSONArray("listing")),
                            true));
                    submitEvent(new LogEventImpl(parseMessageArray(data.getJSONArray("log"))));
                    break;
                case "get-message-reply":
                    submitEvent(new LogEventImpl(Collections.singletonList(parseMessage(data))));
                    break;
                case "log-reply":
                    submitEvent(new LogEventImpl(parseMessageArray(data.getJSONArray("log"))));
                    break;
                case "who-reply":
                    submitEvent(new PresenceChangeEventImpl(parseSessionViewArray(data.getJSONArray("listing")),
                            true));
                    break;
            }
        } catch (JSONException e) {
            Log.e("EuphoriaWebSocketClient", type + " packet missing required fields", e);
        }
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
            return -1;
        }
        return seq;
    }

    private void submitEvent(ConnectionEvent evt) {
        if (evt instanceof PresenceChangeEvent) {
            PresenceChangeEvent e = (PresenceChangeEvent) evt;
            if (e.isPresent()) {
                for (SessionView s : e.getSessions()) sessions.remove(s.getSessionID());
            } else {
                for (SessionView s : e.getSessions()) sessions.put(s.getSessionID(), s);
            }
        } else if (evt instanceof NickChangeEvent) {
            NickChangeEvent e = (NickChangeEvent) evt;
            sessions.put(e.getSession().getSessionID(), e.getSession());
        }
        parent.submitEvent(evt);
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

    private static SessionView parseSessionView(final JSONObject source) throws JSONException {
        return new SessionViewImpl(source.getString("session_id"), source.getString("id"),
                source.getString("name"), source.optBoolean("is_staff"), source.optBoolean("is_manager"));
    }

    private static Message parseMessage(final JSONObject source) throws JSONException {
        return new MessageImpl(source.getString("id"), source.getString("parent"), source.getLong("timestamp"),
                parseSessionView(source.getJSONObject("sender")), source.getString("content"),
                source.optBoolean("truncated"));
    }

    private static List<SessionView> parseSessionViewArray(JSONArray source) throws JSONException {
        List<SessionView> accum = new ArrayList<>();
        for (int i = 0; i < source.length(); i++) {
            accum.add(parseSessionView(source.getJSONObject(i)));
        }
        return Collections.unmodifiableList(accum);
    }

    private static List<Message> parseMessageArray(JSONArray source) throws JSONException {
        List<Message> accum = new ArrayList<>();
        for (int i = 0; i < source.length(); i++) {
            accum.add(parseMessage(source.getJSONObject(i)));
        }
        return Collections.unmodifiableList(accum);
    }

}
