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
    private UIListenerImpl listener;

    public RoomController(RoomUIManager mgr, Context ctx) {
        this.manager = mgr;
        this.context = ctx;
        start();
        bind();
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
                drain();
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

    private void bind() {
        listener = new UIListenerImpl(new Runnable() {
            @Override
            public void run() {
                drain();
            }
        });
        manager.addEventListener(listener);
    }

    private synchronized void drain() {
        if (service != null) service.consume(listener.getEvents());
    }

    public void shutdown() {
        if (service != null) {
            context.unbindService(connection);
            service = null;
            connection = null;
        }
    }
}
