package io.euphoria.xkcd.app.data;

/** Created by Xyzzy on 2017-02-24. */

/* A capture of the state of a session */
public interface SessionView {

    /* The unique session ID */
    String getSessionID();

    /* The persistent user identification */
    String getAgentID();

    /* The current nickname */
    String getName();

    /* Whether the user is a staff member */
    boolean isStaff();

    /* Whether the user is a moderator */
    boolean isManager();

}
