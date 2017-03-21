package io.euphoria.xkcd.app.control;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.euphoria.xkcd.app.connection.Connection;
import io.euphoria.xkcd.app.connection.ConnectionManager;
import io.euphoria.xkcd.app.connection.event.NickChangeEvent;
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
    private final Map<String, RoomUIEventQueue> roomEvents = new HashMap<>();
    private ConnectionManager mgr;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return BINDER;
    }

    @Override
    public void onCreate() {
        mgr = ConnectionManagerImpl.getInstance();
    }

    @Override
    public void onDestroy() {
        mgr.shutdown();
    }

    public void consume(List<UIEvent> events) {
        Set<RoomUIEventQueue> updated = new HashSet<>();
        for (UIEvent evt : events) {
            String roomName;
            /* Room switch events...
             * (a) are not attached to the room they come *from* and
             * (b) have no associated RoomUI at all,
             * hence we push them into the queue for the new room, which allows us to allocate new rooms elegantly,
             * i.e. as soon as events for them arrive. */
            if (evt instanceof RoomSwitchEvent) {
                roomName = ((RoomSwitchEvent) evt).getRoomName();
            } else {
                roomName = evt.getRoomUI().getRoomName();
            }
            RoomUIEventQueue queue = roomEvents.get(roomName);
            if (queue == null) {
                queue = new RoomUIEventQueue(roomName);
                roomEvents.put(roomName, queue);
            }
            queue.put(evt);
            updated.add(queue);
        }
        for (RoomUIEventQueue queue : updated) {
            Connection conn = null;
            for (UIEvent evt : queue.getAll()) {
                String room = queue.getRoomName();
                if (conn == null) {
                    if (evt instanceof RoomSwitchEvent) {
                        conn = mgr.connect(room);
                    } else {
                        conn = mgr.getConnection(room);
                    }
                    if (conn == null) {
                        Log.e("ConnectionService", "Non-connection UI event for nonexistent connection; dropping.");
                        continue;
                    }
                }
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
}
