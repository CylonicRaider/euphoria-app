package io.euphoria.xkcd.app.control;

import android.content.Context;
import android.content.Intent;

import io.euphoria.xkcd.app.ui.RoomUIManager;

/** Created by Xyzzy on 2017-03-19. */

public class RoomController {
    private final RoomUIManager manager;
    private final Context context;

    public RoomController(RoomUIManager mgr, Context ctx) {
        this.manager = mgr;
        this.context = ctx;
        start();
    }

    public RoomUIManager getManager() {
        return manager;
    }

    public Context getContext() {
        return context;
    }

    private void start() {
        context.startService(new Intent(context, ConnectionService.class));
    }
}
