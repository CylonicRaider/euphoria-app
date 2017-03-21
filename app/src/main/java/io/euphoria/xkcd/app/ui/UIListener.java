package io.euphoria.xkcd.app.ui;

import io.euphoria.xkcd.app.ui.event.CloseEvent;
import io.euphoria.xkcd.app.ui.event.LogRequestEvent;
import io.euphoria.xkcd.app.ui.event.MessageSendEvent;
import io.euphoria.xkcd.app.ui.event.NewNickEvent;
import io.euphoria.xkcd.app.ui.event.RoomSwitchEvent;

/** Created by Xyzzy on 2017-02-26. */

/* Listener interface for UI events */
public interface UIListener {

    /* The user intends to change the nickname */
    void onNewNick(NewNickEvent evt);

    /* The user intends to send a message */
    void onMessageSend(MessageSendEvent evt);

    /* The user wishes to see more room logs */
    void onLogRequest(LogRequestEvent evt);

    /* The user intends to change to another room */
    void onRoomSwitch(RoomSwitchEvent evt);

    /* The user intends to close the room */
    void onClose(CloseEvent evt);

}
