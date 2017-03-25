package io.euphoria.xkcd.app.control;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.euphoria.xkcd.app.connection.Connection;
import io.euphoria.xkcd.app.connection.ConnectionManager;
import io.euphoria.xkcd.app.connection.event.ConnectionEvent;
import io.euphoria.xkcd.app.connection.event.NickChangeEvent;
import io.euphoria.xkcd.app.connection.event.OpenEvent;
import io.euphoria.xkcd.app.impl.connection.ConnectionManagerImpl;
import io.euphoria.xkcd.app.ui.event.CloseEvent;
import io.euphoria.xkcd.app.ui.event.LogRequestEvent;
import io.euphoria.xkcd.app.ui.event.MessageSendEvent;
import io.euphoria.xkcd.app.ui.event.RoomSwitchEvent;
import io.euphoria.xkcd.app.ui.event.UIEvent;

/** Created by Xyzzy on 2017-03-19. */

public class ConnectionService extends Service {
    public class CBinder extends Binder {
        public ConnectionService getService() {
            return ConnectionService.this;
        }
    }

    private final CBinder BINDER = new CBinder();
    private final Runnable TERMINATE = new Runnable() {
        @Override
        public void run() {
            stopSelf();
        }
    };
    private final PausingEventQueue<ConnectionEvent> backEvents = new PausingEventQueue<>(new Runnable() {
        @Override
        public void run() {
            drain();
        }
    });
    private final Map<String, RoomUIEventQueue> roomEvents = new HashMap<>();
    private RoomController bound;
    private ConnectionManager mgr;

    @Override
    public void onCreate() {
        mgr = ConnectionManagerImpl.getInstance();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return BINDER;
    }

    public void addBinding(RoomController controller) {
        if (bound != null) throw new IllegalStateException("Service already bound");
        bound = controller;
        backEvents.setPaused(false);
    }

    public void removeBinding() {
        if (bound == null) throw new IllegalStateException("Service not bound");
        bound = null;
        backEvents.setPaused(true);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        final long deadline = System.currentTimeMillis();
        Handler hnd = new Handler(getMainLooper());
        hnd.postDelayed(TERMINATE, 10000);
        hnd.postDelayed(new Runnable() {
            @Override
            public void run() {
                Iterator<Map.Entry<String, RoomUIEventQueue>> iter = roomEvents.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<String, RoomUIEventQueue> ent = iter.next();
                    if (! ent.getValue().touchedSince(deadline)) iter.remove();
                }
            }
        }, 10000);
        return true;
    }

    @Override
    public void onRebind(Intent intent) {
        new Handler(getMainLooper()).removeCallbacks(TERMINATE);
    }

    @Override
    public void onDestroy() {
        mgr.shutdown();
    }

    private void drain() {
        RoomController controller;
        synchronized (this) {
            controller = bound;
        }
        if (controller != null) controller.consume(backEvents.getAll());
    }

    void consume(List<UIEvent> events) {
        Set<String> updated = new HashSet<>();
        for (UIEvent evt : events) {
            final String roomName;
            /* Room switch events...
             * (a) are not attached to the room they come *from* and
             * (b) have no associated RoomUI at all,
             * hence we push them into the queue for the new room, which allows us to allocate new rooms elegantly,
             * i.e. as soon as events for them arrive. */
            if (evt instanceof RoomSwitchEvent) {
                roomName = ((RoomSwitchEvent) evt).getRoomName();
                if (mgr.getConnection(roomName) == null) {
                    mgr.connect(roomName).addEventListener(new ConnectionListenerImpl(backEvents) {
                        @Override
                        public void onOpen(OpenEvent evt) {
                            super.onOpen(evt);
                            drain(evt.getConnection().getRoomName());
                        }
                    });
                }
            } else {
                roomName = evt.getRoomUI().getRoomName();
            }
            RoomUIEventQueue queue = roomEvents.get(roomName);
            if (queue == null) {
                queue = new RoomUIEventQueue(new Runnable() {
                    @Override
                    public void run() {
                        drain(roomName);
                    }
                });
                roomEvents.put(roomName, queue);
            }
            queue.add(evt);
            updated.add(roomName);
        }
        for (String roomName : updated) drain(roomName);
    }

    private void drain(String roomName) {
        Connection conn = mgr.getConnection(roomName);
        if (conn == null) {
            Log.e("ConnectionService", "Events pending for nonexistent connection; ignoring.");
            return;
        }
        if (! conn.getStatus().isConnected()) return;
        for (UIEvent evt : roomEvents.get(roomName).getAll()) {
            if (evt instanceof NickChangeEvent) {
                conn.setNick(((NickChangeEvent) evt).getNewNick());
            } else if (evt instanceof MessageSendEvent) {
                MessageSendEvent e = (MessageSendEvent) evt;
                conn.postMessage(e.getText(), e.getParent());
            } else if (evt instanceof LogRequestEvent) {
                LogRequestEvent e = (LogRequestEvent) evt;
                conn.requestLogs(e.getBefore(), 100);
            } else if (evt instanceof RoomSwitchEvent) {
                /* NOP */
            } else if (evt instanceof CloseEvent) {
                conn.close();
            } else {
                Log.e("ConnectionService", "Unknown UI event class; dropping.");
            }
        }
    }
}
