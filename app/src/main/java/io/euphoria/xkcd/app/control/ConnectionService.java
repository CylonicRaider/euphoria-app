package io.euphoria.xkcd.app.control;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.euphoria.xkcd.app.connection.ConnectionManager;
import io.euphoria.xkcd.app.impl.connection.ConnectionManagerImpl;
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

    public void consume(List<EventWrapper<? extends UIEvent>> events) {
        for (EventWrapper<? extends UIEvent> evt : events) {
            String roomName;
            /* Room switch events...
             * (a) are not attached to the room they come *from* and
             * (b) have no associated RoomUI at all,
             * hence we push them into the queue for the new room, which allows us to allocate new rooms elegantly,
             * i.e. as soon as events for them arrive. */
            if (evt.getEvent() instanceof RoomSwitchEvent) {
                roomName = ((RoomSwitchEvent) evt.getEvent()).getRoomName();
            } else {
                roomName = evt.getEvent().getRoomUI().getRoomName();
            }
            RoomUIEventQueue queue = roomEvents.get(roomName);
            if (queue == null) {
                queue = new RoomUIEventQueue(roomName);
                roomEvents.put(roomName, queue);
            }
            queue.put(evt);
        }
    }
}
