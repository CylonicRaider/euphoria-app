package io.euphoria.xkcd.app.control;

import java.util.List;

import io.euphoria.xkcd.app.connection.ConnectionListener;
import io.euphoria.xkcd.app.connection.event.CloseEvent;
import io.euphoria.xkcd.app.connection.event.ConnectionEvent;
import io.euphoria.xkcd.app.connection.event.IdentityEvent;
import io.euphoria.xkcd.app.connection.event.LogEvent;
import io.euphoria.xkcd.app.connection.event.MessageEvent;
import io.euphoria.xkcd.app.connection.event.NickChangeEvent;
import io.euphoria.xkcd.app.connection.event.OpenEvent;
import io.euphoria.xkcd.app.connection.event.PresenceChangeEvent;

/** Created by Xyzzy on 2017-03-22. */

public class ConnectionListenerImpl implements ConnectionListener {
    private final EventQueue<ConnectionEvent> queue;

    public ConnectionListenerImpl(Runnable schedule) {
        queue = new EventQueue<>(schedule);
    }

    @Override
    public void onOpen(OpenEvent evt) {
        queue.add(evt);
    }

    @Override
    public void onIdentity(IdentityEvent evt) {
        queue.add(evt);
    }

    @Override
    public void onNickChange(NickChangeEvent evt) {
        queue.add(evt);
    }

    @Override
    public void onMessage(MessageEvent evt) {
        queue.add(evt);
    }

    @Override
    public void onPresenceChange(PresenceChangeEvent evt) {
        queue.add(evt);
    }

    @Override
    public void onLogEvent(LogEvent evt) {
        queue.add(evt);
    }

    @Override
    public void onClose(CloseEvent evt) {
        queue.add(evt);
    }

    public List<ConnectionEvent> getEvents() {
        return queue.getAll();
    }

}
