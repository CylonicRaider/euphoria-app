package io.euphoria.xkcd.app.control;

import java.util.List;

import io.euphoria.xkcd.app.ui.UIListener;
import io.euphoria.xkcd.app.ui.UIManagerListener;
import io.euphoria.xkcd.app.ui.event.CloseEvent;
import io.euphoria.xkcd.app.ui.event.LogRequestEvent;
import io.euphoria.xkcd.app.ui.event.MessageSendEvent;
import io.euphoria.xkcd.app.ui.event.NewNickEvent;
import io.euphoria.xkcd.app.ui.event.RoomSwitchEvent;
import io.euphoria.xkcd.app.ui.event.UIEvent;

/** Created by Xyzzy on 2017-03-20. */

public class UIListenerImpl implements UIListener, UIManagerListener {
    private final EventQueue<EventWrapper<? extends UIEvent>> queue;

    public UIListenerImpl(Runnable requeue) {
        queue = new EventQueue<>(requeue);
    }

    @Override
    public void onNewNick(NewNickEvent evt) {
        queue.put(new EventWrapper<>(NewNickEvent.class, evt));
    }

    @Override
    public void onMessageSend(MessageSendEvent evt) {
        queue.put(new EventWrapper<>(MessageSendEvent.class, evt));
    }

    @Override
    public void onLogRequest(LogRequestEvent evt) {
        queue.put(new EventWrapper<>(LogRequestEvent.class, evt));
    }

    @Override
    public void onRoomSwitch(RoomSwitchEvent evt) {
        queue.put(new EventWrapper<>(RoomSwitchEvent.class, evt));
    }

    @Override
    public void onClose(CloseEvent evt) {
        queue.put(new EventWrapper<>(CloseEvent.class, evt));
    }

    public List<EventWrapper<? extends UIEvent>> getEvents() {
        return queue.getAll();
    }

}
