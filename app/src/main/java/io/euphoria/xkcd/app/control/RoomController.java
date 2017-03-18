package io.euphoria.xkcd.app.control;

import io.euphoria.xkcd.app.ui.RoomUIManager;

/** Created by Xyzzy on 2017-03-19. */

public class RoomController {
    private final RoomUIManager manager;

    public RoomController(RoomUIManager mgr) {
        this.manager = mgr;
    }

    public RoomUIManager getManager() {
        return manager;
    }
}
