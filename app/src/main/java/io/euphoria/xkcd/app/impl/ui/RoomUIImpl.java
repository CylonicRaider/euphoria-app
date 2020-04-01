package io.euphoria.xkcd.app.impl.ui;

import android.net.Uri;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.Log;
import android.widget.TextView;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.euphoria.xkcd.app.R;
import io.euphoria.xkcd.app.connection.ConnectionStatus;
import io.euphoria.xkcd.app.data.Message;
import io.euphoria.xkcd.app.data.SessionView;
import io.euphoria.xkcd.app.ui.RoomUI;
import io.euphoria.xkcd.app.ui.UIListener;
import io.euphoria.xkcd.app.ui.event.LogRequestEvent;
import io.euphoria.xkcd.app.ui.event.MessageSendEvent;
import io.euphoria.xkcd.app.ui.event.NewNickEvent;
import io.euphoria.xkcd.app.ui.event.RoomSwitchEvent;
import io.euphoria.xkcd.app.ui.event.UICloseEvent;
import io.euphoria.xkcd.app.ui.event.UIEvent;

public class RoomUIImpl implements RoomUI {

    /* Loosened variations of the patterns in backend/handlers.go as of 097b7da2e0b23e9c5828c0e4831a3de660bb5302.
     * They are lowercase-only indeed. */
    private static final Pattern ROOM_FRAG_RE = Pattern.compile("[a-z0-9:]*");
    private static final Pattern ROOM_NAME_RE = Pattern.compile("(?:[a-z]+:)?[a-z0-9]+");
    private static final Pattern ROOM_PATH_RE = Pattern.compile("/room/(" + ROOM_NAME_RE.pattern() + ")/?");

    private final String roomName;
    private final Set<UIListener> listeners = new LinkedHashSet<>();
    private TextView statusDisplay;
    private MessageListAdapter messagesAdapter;
    private UserListAdapter usersAdapter;

    public RoomUIImpl(String roomName) {
        this.roomName = roomName;
    }

    @Override
    public String getRoomName() {
        return roomName;
    }

    @Override
    public void show() {
        logNYI("Showing a room");
    }

    @Override
    public void close() {
        logNYI("Closing a room");
    }

    @Override
    public void setConnectionStatus(ConnectionStatus status) {
        @ColorRes int color = R.color.status_unknown;
        @StringRes int content = R.string.status_unknown;
        switch (status) {
            case DISCONNECTED:
                color = R.color.status_disconnected;
                content = R.string.status_disconnected;
                break;
            case CONNECTING:
                color = R.color.status_connecting;
                content = R.string.status_connecting;
                break;
            case RECONNECTING:
                color = R.color.status_reconnecting;
                content = R.string.status_reconnecting;
                break;
            case CONNECTED:
                color = R.color.status_connected;
                content = R.string.status_connected;
                break;
        }
        // This one is called after unlinking; handle that case gracefully.
        if (statusDisplay == null) {
            Log.e("RoomUIImpl", "Lost connection status update: " + status);
            return;
        }
        statusDisplay.setTextColor(UIUtils.getColor(statusDisplay.getContext(), color));
        statusDisplay.setText(content);
    }

    @Override
    public void showMessages(List<Message> messages) {
        for (Message m : messages) {
            messagesAdapter.add(new UIMessage(m));
        }
    }

    @Override
    public void showNicks(List<SessionView> sessions) {
        usersAdapter.getData().addAll(sessions);
    }

    @Override
    public void removeNicks(List<SessionView> sessions) {
        usersAdapter.getData().removeAll(sessions);
    }

    /**
     * Adds an UIListener to the RoomUIImpl.
     * If the listener object is already registered,
     * the method will not register it again.
     *
     * @param l Listener to add
     */
    @Override
    public void addEventListener(@NonNull UIListener l) {
        listeners.add(l);
    }

    /**
     * Removes an UIListener from the RoomUIImpl.
     * If the listener object is not registered,
     * the method will change nothing.
     *
     * @param l Listener to remove
     */
    @Override
    public void removeEventListener(@NonNull UIListener l) {
        listeners.remove(l);
    }

    private static void logNYI(String detail) {
        Log.e("RoomUIImpl", detail + " is not yet implemented...");
    }

    public void link(TextView status, MessageListAdapter messages, UserListAdapter users) {
        statusDisplay = status;
        messagesAdapter = messages;
        usersAdapter = users;
    }

    public void unlink(TextView status, MessageListAdapter messages, UserListAdapter users) {
        if (statusDisplay == status) statusDisplay = null;
        if (messagesAdapter == messages) messagesAdapter = null;
        if (usersAdapter == users) usersAdapter = null;
    }

    public void submitEvent(UIEvent evt) {
        for (UIListener l : listeners) {
            if (evt instanceof NewNickEvent) {
                l.onNewNick((NewNickEvent) evt);
            } else if (evt instanceof MessageSendEvent) {
                l.onMessageSend((MessageSendEvent) evt);
            } else if (evt instanceof LogRequestEvent) {
                l.onLogRequest((LogRequestEvent) evt);
            } else if (evt instanceof RoomSwitchEvent) {
                l.onRoomSwitch((RoomSwitchEvent) evt);
            } else if (evt instanceof UICloseEvent) {
                l.onClose((UICloseEvent) evt);
            } else {
                Log.e("RoomUIImpl", "Unknown UI event " + evt + "; dropping.");
            }
        }
    }

    /**
     * Test whether the given URI denotes a valid Euphoria room.
     *
     * @param uri The URI to test
     * @return The test result
     */
    public static boolean isValidRoomUri(@Nullable Uri uri) {
        return uri != null && Pattern.matches("https?", uri.getScheme()) &&
                "euphoria.io".equalsIgnoreCase(uri.getAuthority()) &&
                uri.getPath() != null && ROOM_PATH_RE.matcher(uri.getPath()).matches();
        // Query strings and fragment identifiers are allowed.
    }

    /**
     * Test whether the given characters are a potentially valid component of a room name.
     * Useful for, e.g., pre-filtering text fields.
     *
     * @param chars Characters to test
     * @return The test result
     */
    public static boolean isValidRoomNameFragment(String chars) {
        return ROOM_FRAG_RE.matcher(chars).matches();
    }

    /**
     * Test whether the given string is a valid Euphoria room name.
     *
     * @param name The string to test
     * @return The test result
     */
    public static boolean isValidRoomName(String name) {
        return ROOM_NAME_RE.matcher(name).matches();
    }

    /**
     * Retrieve the room name from the given URI.
     *
     * @param uri The URI to probe
     * @return The room name, or <code>null</code> if the room name cannot be isolated
     */
    public static String getRoomName(@NonNull Uri uri) {
        Matcher m = ROOM_PATH_RE.matcher(uri.getPath());
        if (!m.matches()) return null;
        return m.group(1);
    }

}
