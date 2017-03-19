package io.euphoria.xkcd.app.control;

import android.content.Context;

import io.euphoria.xkcd.app.ui.RoomUIManager;

/** Created by Xyzzy on 2017-03-19. */

public class RoomController {
    private final RoomUIManager manager;
    private final Context context;

    public RoomController(RoomUIManager mgr, Context ctx) {
        this.manager = mgr;
        this.context = ctx;
    }

    public RoomUIManager getManager() {
        return manager;
    }

    public Context getContext() {
        return context;
    }

    public void shutdown() {
        throw new AssertionError("Not implemented");
    }
}
