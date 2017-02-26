package io.euphoria.xkcd.app.connection.event;

import java.util.List;

import io.euphoria.xkcd.app.data.SessionView;

/** Created by Xyzzy on 2017-02-24. */

/* Event encapsulating a presence change */
public interface PresenceChangeEvent extends ConnectionEvent {

    /* The presence changes announced */
    List<SessionView> getSessions();

    /* Whether the sessions joined or left */
    boolean isPresent();

}
