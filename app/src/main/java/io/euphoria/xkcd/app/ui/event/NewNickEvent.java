package io.euphoria.xkcd.app.ui.event;

/** Created by Xyzzy on 2017-02-26. */

/* Event encapsulating the intent to change to another nick */
public interface NewNickEvent extends UIEvent {

    /* The nickname to change to */
    String getNewNick();

}
