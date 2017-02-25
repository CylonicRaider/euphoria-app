package io.euphoria.xkcd.app.connection.event;

import io.euphoria.xkcd.app.data.SessionView;

/* Created by Xyzzy on 2017-02-25. */

/* An event informing about the (own) identity of a client */
public interface IdentityEvent extends ConnectionEvent {

    /* The identity of the client */
    SessionView getIdentity();

}
