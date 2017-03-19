package io.euphoria.xkcd.app.ui;

import io.euphoria.xkcd.app.ui.event.RoomSwitchEvent;

/** Created by Xyzzy on 2017-03-19. */

/* Listener for room-agnostic UI events
 *
 * Such events may stem, for example, from an Intent being delivered to the app from outside, or from the (internal!)
 * room selection screen. */
public interface UIManagerListener {

    /* The user intends to change to another room */
    void onRoomSwitch(RoomSwitchEvent evt);

}
