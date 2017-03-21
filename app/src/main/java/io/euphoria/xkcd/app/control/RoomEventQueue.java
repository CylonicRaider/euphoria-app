package io.euphoria.xkcd.app.control;

import io.euphoria.xkcd.app.control.EventQueue;
import io.euphoria.xkcd.app.control.EventWrapper;
import io.euphoria.xkcd.app.ui.event.UIEvent;

/** Created by Xyzzy on 2017-03-21. */


public class RoomEventQueue extends EventQueue<EventWrapper<? extends UIEvent>> {
    private final String roomName;

    public RoomEventQueue(String roomName, Runnable requeue) {
        super(requeue);
        this.roomName = roomName;
    }
    public RoomEventQueue(String roomName) {
        this(roomName, null);
    }

    public String getRoomName() {
        return roomName;
    }
}
