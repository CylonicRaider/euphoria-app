package io.euphoria.xkcd.app.ui

/** Created by Xyzzy on 2020-04-01.  */ /* Factory interface for RoomUI-s */
interface RoomUIFactory {
    /* Create a RoomUI for the given room name */
    fun createRoomUI(roomName: String): RoomUI
}
