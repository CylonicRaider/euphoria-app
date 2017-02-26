package io.euphoria.xkcd.app.ui.event;

/** Created by Xyzzy on 2017-02-26. */

/* Event encapsulating the intent to send a message */
public interface MessageSendEvent extends UIEvent {

    /* The text of the message to be sent */
    String getText();

    /* The parent of the message, or null for none */
    String getParent();

}
