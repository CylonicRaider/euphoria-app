package io.euphoria.xkcd.app.data

/** Created by Xyzzy on 2017-02-24.  */ /* A capture of the state of a session */
interface SessionView {
    /* The unique session ID */
    val sessionID: String

    /* The persistent user identification */
    val agentID: String

    /* The current nickname */
    val name: String?

    /* Whether the user is a staff member */
    val isStaff: Boolean

    /* Whether the user is a moderator */
    val isManager: Boolean
}
