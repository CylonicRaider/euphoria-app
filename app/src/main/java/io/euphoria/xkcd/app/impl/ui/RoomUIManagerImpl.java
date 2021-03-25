package io.euphoria.xkcd.app.impl.ui;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.euphoria.xkcd.app.ui.RoomUI;
import io.euphoria.xkcd.app.ui.RoomUIFactory;
import io.euphoria.xkcd.app.ui.RoomUIManager;
import io.euphoria.xkcd.app.ui.UIManagerListener;
import io.euphoria.xkcd.app.ui.event.RoomSwitchEvent;

/** Created by Xyzzy on 2017-02-24. */

/* Implementation of RoomUIManager */
public class RoomUIManagerImpl implements RoomUIManager {

    private class DefaultRoomUIFactory implements RoomUIFactory {

        @Override
        public RoomUI createRoomUI(String roomName) {
            return new RoomUIImpl(RoomUIManagerImpl.this, roomName);
        }

    }

    private final Set<UIManagerListener> listeners = new HashSet<>();
    private final HashMap<String, RoomUI> roomUIs = new HashMap<>();
    private final List<String> openRooms = new ArrayList<>();
    private RoomUIFactory factory = new DefaultRoomUIFactory();
    private String currentRoom;

    @Override
    public void setRoomUIFactory(RoomUIFactory factory) {
        if (factory == null) factory = new DefaultRoomUIFactory();
        if (factory != this.factory) roomUIs.clear();
        this.factory = factory;
    }

    @Override
    public RoomUI getRoomUI(@NonNull String roomName) {
        if (roomUIs.containsKey(roomName)) {
            return roomUIs.get(roomName);
        } else {
            RoomUI ret = factory.createRoomUI(roomName);
            roomUIs.put(roomName, ret);
            UIUtils.insertSorted(openRooms, roomName);
            return ret;
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

    public void selectRoom(final String roomName) {
        currentRoom = roomName;
        RoomSwitchEvent evt = new RoomSwitchEvent() {
            @Override
            public String getRoomName() {
                return roomName;
            }

            @Override
            public RoomUI getRoomUI() {
                return RoomUIManagerImpl.this.getRoomUI(roomName);
            }
        };
        for (UIManagerListener listener : listeners) {
            listener.onRoomSwitch(evt);
        }
    }

    public void closeRoom(String roomName) {
        int oldIndex = UIUtils.removeSorted(openRooms, roomName);
        if (!roomName.equals(currentRoom) || openRooms.isEmpty()) return;
        if (oldIndex == -1) oldIndex = 0;
        selectRoom(openRooms.get(oldIndex));
    }

}
