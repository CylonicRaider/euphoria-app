package io.euphoria.xkcd.app.control;

import io.euphoria.xkcd.app.ui.event.UIEvent;

/** Created by Xyzzy on 2017-03-21. */

public class RoomUIEventQueue extends EventQueue<EventWrapper<? extends UIEvent>> {
    private final String roomName;

    public RoomUIEventQueue(String roomName, Runnable schedule) {
        super(schedule);
        this.roomName = roomName;
    }
    public RoomUIEventQueue(String roomName) {
        this(roomName, null);
    }

    public String getRoomName() {
        return roomName;
    }
}
