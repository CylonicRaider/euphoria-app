package io.euphoria.xkcd.app.impl.ui;

import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.util.Log;
import android.widget.TextView;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import io.euphoria.xkcd.app.R;
import io.euphoria.xkcd.app.connection.ConnectionStatus;
import io.euphoria.xkcd.app.data.Message;
import io.euphoria.xkcd.app.data.SessionView;
import io.euphoria.xkcd.app.impl.ui.data.RoomState;
import io.euphoria.xkcd.app.impl.ui.data.UIMessage;
import io.euphoria.xkcd.app.impl.ui.views.InputBarView;
import io.euphoria.xkcd.app.impl.ui.views.MessageListAdapter;
import io.euphoria.xkcd.app.impl.ui.views.UserListAdapter;
import io.euphoria.xkcd.app.ui.RoomUI;
import io.euphoria.xkcd.app.ui.UIListener;
import io.euphoria.xkcd.app.ui.event.LogRequestEvent;
import io.euphoria.xkcd.app.ui.event.MessageSendEvent;
import io.euphoria.xkcd.app.ui.event.NewNickEvent;
import io.euphoria.xkcd.app.ui.event.RoomSwitchEvent;
import io.euphoria.xkcd.app.ui.event.UICloseEvent;
import io.euphoria.xkcd.app.ui.event.UIEvent;

public class RoomUIImpl implements RoomUI {

    private static final String TAG = "RoomUIImpl";

    private final RoomUIManagerImpl parent;
    private final String roomName;
    private final Set<UIListener> listeners = new LinkedHashSet<>();
    private final RoomState saveState = new RoomState();
    private SessionView identity;
    private ConnectionStatus currentStatus;
    private TextView statusDisplay;
    private MessageListAdapter messagesAdapter;
    private UserListAdapter usersAdapter;
    private InputBarView inputBar;

    public RoomUIImpl(RoomUIManagerImpl parent, String roomName) {
        this.parent = parent;
        this.roomName = roomName;
    }

    @Override
    public String getRoomName() {
        return roomName;
    }

    @Override
    public void show() {
        parent.selectRoom(roomName);
    }

    @Override
    public void close() {
        parent.closeRoom(roomName);
    }

    @Override
    public void setConnectionStatus(ConnectionStatus status) {
        currentStatus = status;
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
            Log.e(TAG, "Lost connection status update: " + status);
            return;
        }
        statusDisplay.setTextColor(UIUtils.getColor(statusDisplay.getContext(), color));
        statusDisplay.setText(content);
    }

    @Override
    public void setIdentity(SessionView identity) {
        this.identity = identity;
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
        for (SessionView s : sessions) {
            if (s.getSessionID().equals(identity.getSessionID())) {
                inputBar.setAllNicks(s.getName());
            }
        }
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

    public RoomState getSaveState() {
        return saveState;
    }

    public void swapIn(RoomUIImpl awayFrom) {
        RoomState saveTo;
        if (awayFrom == null) {
            Log.w(TAG, "Swapping in from unknown room?!");
            saveTo = new RoomState();
        } else {
            saveTo = awayFrom.getSaveState();
        }
        messagesAdapter.swap(saveTo, saveState);
        usersAdapter.swap(saveTo, saveState);
        setConnectionStatus(currentStatus);
    }

    public void link(TextView status, MessageListAdapter messages, UserListAdapter users, InputBarView input) {
        statusDisplay = status;
        messagesAdapter = messages;
        usersAdapter = users;
        inputBar = input;
    }

    public void unlink(TextView status, MessageListAdapter messages, UserListAdapter users, InputBarView input) {
        if (statusDisplay == status) statusDisplay = null;
        if (messagesAdapter == messages) messagesAdapter = null;
        if (usersAdapter == users) usersAdapter = null;
        if (inputBar == input) inputBar = null;
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

}
