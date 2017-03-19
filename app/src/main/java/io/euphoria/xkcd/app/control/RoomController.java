package io.euphoria.xkcd.app.control;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import io.euphoria.xkcd.app.ui.RoomUIManager;

/** Created by Xyzzy on 2017-03-19. */

public class RoomController {
    private final RoomUIManager manager;
    private final Context context;
    private ConnectionService service;
    private ServiceConnection connection;

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
        Intent intent = new Intent(context, ConnectionService.class);
        context.startService(intent);
        connection = new ServiceConnection() {
            private ConnectionService service;

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                // Wheee! I should name even more variables like that!
                this.service = ((ConnectionService.CBinder) service).getService();
                RoomController.this.service = this.service;
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                // Prevent race condition.
                if (RoomController.this.service == this.service)
                    RoomController.this.service = null;
            }
        };
        // Service already created.
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    public void shutdown() {
        if (service != null) {
            context.unbindService(connection);
            service = null;
            connection = null;
        }
    }
}
