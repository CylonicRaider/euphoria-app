package io.euphoria.xkcd.app.ui

import io.euphoria.xkcd.app.ui.event.*

/** Created by Xyzzy on 2017-02-26.  */ /* Listener interface for UI events */
interface UIListener {
    /* The user intends to change the nickname */
    fun onNewNick(evt: NewNickEvent)

    /* The user intends to send a message */
    fun onMessageSend(evt: MessageSendEvent)

    /* The user wishes to see more room logs */
    fun onLogRequest(evt: LogRequestEvent)

    /* The user intends to change to another room */
    fun onRoomSwitch(evt: RoomSwitchEvent)

    /* The user intends to close the room */
    fun onClose(evt: UICloseEvent)
}
