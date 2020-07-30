package io.euphoria.xkcd.app.impl.connection

import io.euphoria.xkcd.app.data.SessionView

/** Created by Xyzzy on 2017-05-04.  */
open interface ServerSessionView : SessionView {
    /* The (administrative) ID of the server */
    val serverID: String?

    /* The (unique) "era" of the server */
    val serverEra: String?
}
