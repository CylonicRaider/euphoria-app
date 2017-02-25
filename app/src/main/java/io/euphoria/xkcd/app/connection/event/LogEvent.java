package io.euphoria.xkcd.app.connection.event;

import java.util.List;

import io.euphoria.xkcd.app.data.Message;

/* Created by Xyzzy on 2017-02-24. */

/* Event encapsulating a batch of old messages */
public interface LogEvent extends ConnectionEvent {

    /* The substance of this event */
    List<Message> getMessages();

}
