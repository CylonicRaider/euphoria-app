package io.euphoria.xkcd.app.ui.event

/** Created by Xyzzy on 2017-02-26.  */ /* Event encapsulating the intent to change to another room */
interface RoomSwitchEvent : UIEvent {
    /* The room name to change to */
    val roomName: String
}
