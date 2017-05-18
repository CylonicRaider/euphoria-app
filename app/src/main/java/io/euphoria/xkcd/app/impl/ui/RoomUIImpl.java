package io.euphoria.xkcd.app.impl.ui;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.euphoria.xkcd.app.connection.ConnectionStatus;
import io.euphoria.xkcd.app.data.Message;
import io.euphoria.xkcd.app.ui.RoomUI;
import io.euphoria.xkcd.app.ui.UIListener;

/**
 * @author N00bySumairu
 */

// TODO add RecyclerView.Adapter for the top-level messages
public class RoomUIImpl implements RoomUI {

    private String roomName;
    // TODO optionally change to ArrayList, if more efficient
    private Set<UIListener> listeners = new LinkedHashSet<>();
    private Map<String, String> activeSessions = new HashMap<>();
    private Map<String, MessageContainer> messages = new HashMap<>();
    private List<MessageContainer> roots = new ArrayList<>();
    private RootMessageListAdapter adapter = new RootMessageListAdapter();

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

    // TODO Adjust to new MessageContainer
    @Override
    public void showMessages(List<Message> messages) {
        // STUB
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

    public RootMessageListAdapter getAdapter() {
        return adapter;
    }
}
