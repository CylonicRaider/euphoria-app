package io.euphoria.xkcd.app.impl.ui;

import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import io.euphoria.xkcd.app.ui.RoomUI;
import io.euphoria.xkcd.app.ui.RoomUIManager;
import io.euphoria.xkcd.app.ui.UIManagerListener;
import io.euphoria.xkcd.app.ui.event.RoomSwitchEvent;

/** Created by Xyzzy on 2017-02-24. */

/* Implementation of RoomUIManager */
public class RoomUIManagerImpl implements RoomUIManager {

    Set<UIManagerListener> listeners = new HashSet<>();
    HashMap<String, RoomUI> roomUIs = new HashMap<>();

    @Override
    public RoomUI getRoomUI(@NonNull String roomName) {
        if (roomUIs.containsKey(roomName)) {
            return roomUIs.get(roomName);
        } else {
            return new RoomUIImpl(roomName);
        }
    }

    /**
     * Adds an UIManagerListener to the RoomUIManagerImpl.
     * If the listener object is already registered,
     * the method will not register it again.
     *
     * @param l Listener to add
     */
    @Override
    public void addEventListener(@NonNull UIManagerListener l) {
        listeners.add(l);
    }

    /**
     * Removes an UIManagerListener from the RoomUIManagerImpl.
     * If the listener object is not registered,
     * the method will change nothing.
     *
     * @param l Listener to remove
     */
    @Override
    public void removeEventListener(@NonNull UIManagerListener l) {
        listeners.remove(l);
    }

    public void onRoomSwitch(final String roomName) {
        for (UIManagerListener listener :
                listeners) {
            listener.onRoomSwitch(new RoomSwitchEvent() {
                @Override
                public String getRoomName() {
                    return roomName;
                }

                @Override
                public RoomUI getRoomUI() {
                    return RoomUIManagerImpl.this.getRoomUI(roomName);
                }
            });
        }
    }

}
