package io.euphoria.xkcd.app.connection.event;

import java.util.List;

import io.euphoria.xkcd.app.data.Message;

/** Created by Xyzzy on 2017-02-24. */

public interface LogEvent extends ConnectionEvent {

    List<Message> getMessages();

}
