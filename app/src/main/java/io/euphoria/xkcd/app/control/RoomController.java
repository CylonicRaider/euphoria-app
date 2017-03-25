package io.euphoria.xkcd.app.control;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.euphoria.xkcd.app.connection.ConnectionStatus;
import io.euphoria.xkcd.app.connection.event.CloseEvent;
import io.euphoria.xkcd.app.connection.event.ConnectionEvent;
import io.euphoria.xkcd.app.connection.event.IdentityEvent;
import io.euphoria.xkcd.app.connection.event.LogEvent;
import io.euphoria.xkcd.app.connection.event.MessageEvent;
import io.euphoria.xkcd.app.connection.event.NickChangeEvent;
import io.euphoria.xkcd.app.connection.event.OpenEvent;
import io.euphoria.xkcd.app.connection.event.PresenceChangeEvent;
import io.euphoria.xkcd.app.data.SessionView;
import io.euphoria.xkcd.app.ui.RoomUI;
import io.euphoria.xkcd.app.ui.RoomUIManager;

/** Created by Xyzzy on 2017-03-19. */

public class RoomController {
    private final RoomUIManager manager;
    private final Context context;
    private ConnectionService service;
    private ServiceConnection connection;
    private UIListenerImpl uiListener;

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
                this.service.addBinding(RoomController.this);
                drain();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                // Prevent race condition.
                if (RoomController.this.service == this.service)
                    RoomController.this.service = null;
            }
        };
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    private void bind() {
        uiListener = new UIListenerImpl(new Runnable() {
            @Override
            public void run() {
                drain();
            }
        });
        manager.addEventListener(uiListener);
    }

    private synchronized void drain() {
        ConnectionService service;
        synchronized (this) {
            service = this.service;
        }
        if (service != null) service.consume(uiListener.getEvents());
    }

    void consume(List<ConnectionEvent> events) {
        for (ConnectionEvent evt : events) {
            String roomName = evt.getConnection().getRoomName();
            RoomUI ui = manager.getRoomUI(roomName);
            if (evt instanceof OpenEvent) {
                ui.setConnectionStatus(ConnectionStatus.CONNECTED);
            } else if (evt instanceof IdentityEvent) {
                /* NOP */
            } else if (evt instanceof NickChangeEvent) {
                NickChangeEvent e = (NickChangeEvent) evt;
                ui.showNicks(Collections.singletonMap(e.getSession().getSessionID(), e.getNewNick()));
            } else if (evt instanceof MessageEvent) {
                ui.showMessages(Collections.singletonList(((MessageEvent) evt).getMessage()));
            } else if (evt instanceof PresenceChangeEvent) {
                PresenceChangeEvent e = (PresenceChangeEvent) evt;
                if (e.isPresent()) {
                    Map<String, String> users = new HashMap<>();
                    for (SessionView s : e.getSessions()) {
                        users.put(s.getSessionID(), s.getName());
                    }
                    ui.showNicks(users);
                } else {
                    List<String> users = new ArrayList<>();
                    for (SessionView s : e.getSessions()) {
                        users.add(s.getSessionID());
                    }
                    ui.removeNicks(users);
                }
            } else if (evt instanceof LogEvent) {
                ui.showMessages(((LogEvent) evt).getMessages());
            } else if (evt instanceof CloseEvent) {
                if (((CloseEvent) evt).isFinal()) {
                    ui.setConnectionStatus(ConnectionStatus.DISCONNECTED);
                } else {
                    ui.setConnectionStatus(ConnectionStatus.RECONNECTING);
                }
            } else {
                Log.e("RoomController", "Unknown connection event class; dropping.");
            }
        }
    }

    public void shutdown() {
        if (service != null) {
            service.removeBinding();
            context.unbindService(connection);
            service = null;
            connection = null;
        }
    }
}
