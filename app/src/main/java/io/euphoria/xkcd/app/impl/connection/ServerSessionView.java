package io.euphoria.xkcd.app.impl.connection;

import io.euphoria.xkcd.app.data.SessionView;

/** Created by Xyzzy on 2017-05-04. */

public interface ServerSessionView extends SessionView {

    /* The (administrative) ID of the server */
    String getServerID();

    /* The (unique) "era" of the server */
    String getServerEra();

}
