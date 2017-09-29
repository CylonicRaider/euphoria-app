package io.euphoria.xkcd.app.impl.ui;

import android.net.Uri;
import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.euphoria.xkcd.app.connection.ConnectionStatus;
import io.euphoria.xkcd.app.data.Message;
import io.euphoria.xkcd.app.ui.RoomUI;
import io.euphoria.xkcd.app.ui.UIListener;

public class RoomUIImpl implements RoomUI {

    /* Loosened variations of the patterns in backend/handlers.go as of 097b7da2e0b23e9c5828c0e4831a3de660bb5302.
     * They are lowercase-only indeed. */
    private static final Pattern ROOM_FRAG_RE = Pattern.compile("[a-z0-9:]*");
    private static final Pattern ROOM_NAME_RE = Pattern.compile("(?:[a-z]+:)?[a-z0-9]+");
    private static final Pattern ROOM_PATH_RE = Pattern.compile("/room/(" + ROOM_NAME_RE.pattern() + ")/?");

    private String roomName;
    // TODO optionally change to ArrayList, if more efficient
    private Set<UIListener> listeners = new LinkedHashSet<>();
    private Map<String, String> activeSessions = new HashMap<>();

    RoomUIImpl(String roomName) {
        this.roomName = roomName;
    }

    @Override
    public String getRoomName() {
        return roomName;
    }

    @Override
    public void show() {

    }

    @Override
    public void close() {

    }

    @Override
    public void setConnectionStatus(ConnectionStatus status) {

    }

    // TODO Adjust to new MessageListAdapter
    @Override
    public void showMessages(List<Message> messages) {
        /*
        for (Message m : messages) {
            if (this.messages.containsKey(m.getID())) {
                this.messages.get(m.getID()).setMessage(m);
            } else {
                this.messages.put(m.getID(), new MessageContainer());
                roots.add(new MessageContainer(m));
            }
        }*/
    }

    @Override
    public void showNicks(@NonNull Map<String, String> nicknames) {
        activeSessions.putAll(nicknames);
    }

    @Override
    public void removeNicks(@NonNull List<String> sessions) {
        for (String session : sessions) {
            activeSessions.remove(session);
        }
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
     * Test whether the given URI denotes a valid Euphoria room.
     *
     * @param uri The URI to test
     * @return The test result
     */
    public static boolean isValidRoomUri(Uri uri) {
        return Pattern.matches("https?", uri.getScheme()) &&
                "euphoria.io".equalsIgnoreCase(uri.getAuthority()) &&
                ROOM_PATH_RE.matcher(uri.getPath()).matches();
        // Query strings and fragment identifiers are allowed.
    }

    /**
     * Retrieve the room name from the given URI.
     *
     * @param uri The URI to probe
     * @return The room name, or <code>null</code> if the room name cannot be isolated
     */
    public static String getRoomName(Uri uri) {
        Matcher m = ROOM_PATH_RE.matcher(uri.getPath());
        if (!m.matches()) return null;
        return m.group(1);
    }

}
