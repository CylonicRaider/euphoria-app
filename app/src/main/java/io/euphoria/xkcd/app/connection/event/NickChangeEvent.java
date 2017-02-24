package io.euphoria.xkcd.app.connection.event;

import io.euphoria.xkcd.app.data.SessionView;

/** Created by Xyzzy on 2017-02-24. */

public interface NickChangeEvent extends ConnectionEvent {

    /** The session affected (after the nickname change) */
    SessionView getSession();

    /** The old nickname */
    String getOldNick();

    /** The new nickname */
    String getNewNick();

}
