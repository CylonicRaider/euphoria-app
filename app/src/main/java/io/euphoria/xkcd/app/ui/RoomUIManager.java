package io.euphoria.xkcd.app.ui;

/** Created by Xyzzy on 2017-02-24. */

/* Room UI wrapper retrieval hub */
public interface RoomUIManager {

    /* Set the factory for creating RoomUI-s */
    void setRoomUIFactory(RoomUIFactory factory);

    /* Get the room UI for the given name */
    RoomUI getRoomUI(String roomName);

    /* Install an event listener */
    void addEventListener(UIManagerListener l);

    /* Remove an event listener */
    void removeEventListener(UIManagerListener l);

}
