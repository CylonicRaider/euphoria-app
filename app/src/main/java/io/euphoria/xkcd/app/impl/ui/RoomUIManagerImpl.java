package io.euphoria.xkcd.app.impl.ui;

import io.euphoria.xkcd.app.ui.RoomUI;
import io.euphoria.xkcd.app.ui.RoomUIManager;
import io.euphoria.xkcd.app.ui.UIManagerListener;

/** Created by Xyzzy on 2017-02-24. */

/* Implementation of RoomUIManager */
public class RoomUIManagerImpl implements RoomUIManager {

    @Override
    public RoomUI getRoomUI(String roomName) {
        throw new AssertionError("Not implemented");
    }

    @Override
    public void addEventListener(UIManagerListener l) {
        throw new AssertionError("Not implemented");
    }

    @Override
    public void removeEventListener(UIManagerListener l) {
        throw new AssertionError("Not implemented");
    }

}
