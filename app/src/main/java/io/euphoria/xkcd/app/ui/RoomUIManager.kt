package io.euphoria.xkcd.app.ui

/** Created by Xyzzy on 2017-02-24.  */ /* Room UI wrapper retrieval hub */
interface RoomUIManager {
    /* Set the factory for creating RoomUI-s */
    fun setRoomUIFactory(factory: RoomUIFactory?)

    /* Get the room UI for the given name */
    fun getRoomUI(roomName: String): RoomUI

    /* Install an event listener */
    fun addEventListener(l: UIManagerListener)

    /* Remove an event listener */
    fun removeEventListener(l: UIManagerListener)
}
