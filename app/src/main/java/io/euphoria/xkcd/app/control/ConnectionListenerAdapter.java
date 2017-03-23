package io.euphoria.xkcd.app.control;

import io.euphoria.xkcd.app.connection.ConnectionListener;
import io.euphoria.xkcd.app.connection.event.CloseEvent;
import io.euphoria.xkcd.app.connection.event.IdentityEvent;
import io.euphoria.xkcd.app.connection.event.LogEvent;
import io.euphoria.xkcd.app.connection.event.MessageEvent;
import io.euphoria.xkcd.app.connection.event.NickChangeEvent;
import io.euphoria.xkcd.app.connection.event.OpenEvent;
import io.euphoria.xkcd.app.connection.event.PresenceChangeEvent;

/** Created by Xyzzy on 2017-03-23. */

public class ConnectionListenerAdapter implements ConnectionListener {
    @Override
    public void onOpen(OpenEvent evt) {}

    @Override
    public void onIdentity(IdentityEvent evt) {}

    @Override
    public void onNickChange(NickChangeEvent evt) {}

    @Override
    public void onMessage(MessageEvent evt) {}

    @Override
    public void onPresenceChange(PresenceChangeEvent evt) {}

    @Override
    public void onLogEvent(LogEvent evt) {}

    @Override
    public void onClose(CloseEvent evt) {}
}
