package io.euphoria.xkcd.app.impl.connection;

import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;

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

    private static class SessionViewImpl implements ServerSessionView {

        private final String sessionID;
        private final String agentID;
        private final String name;
        private final String serverID;
        private final String serverEra;
        private final boolean staff;
        private final boolean manager;

        public SessionViewImpl(String sessionID, String agentID, String name, boolean staff, boolean manager,
                               String serverID, String serverEra) {
            this.sessionID = sessionID;
            this.agentID = agentID;
            this.name = name;
            this.staff = staff;
            this.manager = manager;
            this.serverID = serverID;
            this.serverEra = serverEra;
        }

        public SessionViewImpl(ServerSessionView base, String newName) {
            this(base.getSessionID(), base.getAgentID(), newName, base.isStaff(), base.isManager(),
                    base.getServerID(), base.getServerEra());
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
        public String getServerID() {
            return serverID;
        }

        @Override
        public String getServerEra() {
            return serverEra;
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

        public MessageImpl(String id, String parent, long timestamp, SessionView sender, String content,
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

        private final ServerSessionView session;

        public IdentityEventImpl(ServerSessionView session) {
            this.session = session;
        }

        @Override
        public ServerSessionView getIdentity() {
            return session;
        }

    }

    private class NickChangeEventImpl extends EventImpl implements NickChangeEvent {

        private final ServerSessionView session;
        private final String oldNick;

        private NickChangeEventImpl(ServerSessionView session, String oldNick) {
            this.session = session;
            this.oldNick = oldNick;
        }

        @Override
        public ServerSessionView getSession() {
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

         private final List<ServerSessionView> sessions;

         private final boolean present;

         private PresenceChangeEventImpl(List<ServerSessionView> sessions, boolean present) {
             this.sessions = sessions;
             this.present = present;
         }

         @Override
         @SuppressWarnings({"unchecked", "rawtypes"})
         public List<SessionView> getSessions() {
             // HACK: User is supposed to use that read-only anyway.
             return (List) sessions;
         }

         public List<ServerSessionView> getSessionsEx() {
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
    private final Map<String, ServerSessionView> sessions;
    private final Queue<String> sendaheadQueue;
    private boolean ready;
    private boolean closed;
    private String sessionID;
    private String confirmedNick;

    public EuphoriaWebSocketClient(ConnectionImpl parent, URI endpoint) {
        super(endpoint);
        this.parent = parent;
        this.sessions = new HashMap<>();
        this.sendaheadQueue = new ArrayDeque<>();
        this.ready = false;
        this.closed = false;
        this.sessionID = null;
        this.confirmedNick = null;
    }

    public ConnectionImpl getParent() {
        return parent;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        parent.submitEvent(new OpenEventImpl());
    }

    private void dispatchReady() {
        while (true) {
            String item;
            synchronized (this) {
                if (ready) return;
                item = sendaheadQueue.poll();
                if (item == null) {
                    ready = true;
                    break;
                }
            }
            send(item);
        }
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
                case "ping-event":
                    send(buildJSONObject("type", "ping-reply", "data", buildJSONObject("time", data.getLong("time")))
                            .toString());
                    break;
                case "hello-event":
                    ServerSessionView identity = parseSessionView(data.getJSONObject("session"));
                    sessionID = identity.getSessionID();
                    submitEvent(new IdentityEventImpl(identity));
                    dispatchReady();
                    break;
                case "join-event":
                    submitEvent(new PresenceChangeEventImpl(Collections.singletonList(parseSessionView(data)),
                            true));
                    break;
                case "network-event":
                    String subtype = data.getString("type");
                    if (subtype.equals("partition")) {
                        String serverID = data.getString("server_id"), serverEra = data.getString("server_era");
                        List<ServerSessionView> removed = new ArrayList<>();
                        Iterator<ServerSessionView> iter = sessions.values().iterator();
                        while (iter.hasNext()) {
                            ServerSessionView s = iter.next();
                            if (s.getServerID().equals(serverID) && s.getServerEra().equals(serverEra)) {
                                iter.remove();
                                removed.add(s);
                            }
                        }
                        submitEvent(new PresenceChangeEventImpl(Collections.unmodifiableList(removed), false));
                    } else {
                        Log.w("EuphoriaWebSocketClient", "Unknown network-event subtype: " + subtype);
                    }
                case "nick-event": case "nick-reply":
                    String newNick;
                    ServerSessionView session;
                    if (data == null) {
                        // Error: The server rejected a nickname change -- roll back to the latest confirmed nick.
                        if (type.equals("nick-event")) {
                            Log.e("EuphoriaWebSocketClient", "nick-event contains no data?!");
                            break;
                        }
                        newNick = confirmedNick;
                        session = sessions.get(sessionID);
                        if (session == null) {
                            Log.e("EuphoriaWebSocketClient",
                                    "Cannot locate our own session while processing a failed nick-reply?!");
                            break;
                        }
                    } else {
                        newNick = data.getString("to");
                        confirmedNick = newNick;
                        session = sessions.get(data.getString("session_id"));
                        if (session == null) {
                            Log.e("EuphoriaWebSocketClient", "Dropping nick change of unknown session ID " +
                                    data.getString("session_id") + "!");
                            break;
                        }
                    }
                    submitEvent(new NickChangeEventImpl(new SessionViewImpl(session, newNick), session.getName()));
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
                default:
                    Log.i("EuphoriaWebSocketClient", "Unrecognized packet type " + type + "!");
                    break;
            }
        } catch (JSONException e) {
            Log.e("EuphoriaWebSocketClient", type + " packet missing required fields", e);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.i("EuphoriaWebSocketClient", "WebSocket connection closed (code " + code + "; reason: \"" + reason +
                "\"; " + (remote ? "remote" : "local") + ")");
        doClose(true);
    }

    @Override
    public void onError(Exception ex) {
        Log.e("EuphoriaWebSocketClient", "Error in WebSocket connection:", ex);
        doClose(false);
    }

    public void sendWithQueue(String data) {
        synchronized (this) {
            if (!ready) {
                sendaheadQueue.add(data);
                return;
            }
        }
        send(data);
    }

    public int sendObject(String type, Object... data) {
        int seq = parent.sequence();
        try {
            sendWithQueue(buildJSONObject("type", type, "id", Integer.toString(seq),
                    "data", buildJSONObject(data)).toString());
        } catch (JSONException exc) {
            Log.e("EuphoriaWebSocketClient", "Exception while serializing JSON", exc);
            return -1;
        }
        return seq;
    }

    public void doClose(boolean fin) {
        boolean makeEvent = false;
        synchronized (this) {
            if (! closed) {
                makeEvent = true;
                closed = true;
            }
        }
        if (makeEvent) submitEvent(new CloseEventImpl(fin));
        close();
    }

    private void submitEvent(ConnectionEvent evt) {
        if (evt instanceof IdentityEventImpl) {
            IdentityEventImpl e = (IdentityEventImpl) evt;
            sessions.put(e.getIdentity().getSessionID(), e.getIdentity());
        } else if (evt instanceof PresenceChangeEventImpl) {
            PresenceChangeEventImpl e = (PresenceChangeEventImpl) evt;
            if (e.isPresent()) {
                for (ServerSessionView s : e.getSessionsEx()) sessions.remove(s.getSessionID());
            } else {
                for (ServerSessionView s : e.getSessionsEx()) sessions.put(s.getSessionID(), s);
            }
        } else if (evt instanceof NickChangeEventImpl) {
            NickChangeEventImpl e = (NickChangeEventImpl) evt;
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

    private static ServerSessionView parseSessionView(final JSONObject source) throws JSONException {
        return new SessionViewImpl(source.getString("session_id"), source.getString("id"),
                source.getString("name"), source.optBoolean("is_staff"), source.optBoolean("is_manager"),
                source.getString("server_id"), source.getString("server_era"));
    }

    private static Message parseMessage(final JSONObject source) throws JSONException {
        return new MessageImpl(source.getString("id"), source.optString("parent", null), source.getLong("time"),
                parseSessionView(source.getJSONObject("sender")), source.getString("content"),
                source.optBoolean("truncated"));
    }

    private static List<ServerSessionView> parseSessionViewArray(JSONArray source) throws JSONException {
        List<ServerSessionView> accum = new ArrayList<>();
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
