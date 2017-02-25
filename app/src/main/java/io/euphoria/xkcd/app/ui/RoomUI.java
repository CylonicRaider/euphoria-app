package io.euphoria.xkcd.app.ui;

import java.util.List;
import java.util.Map;

import io.euphoria.xkcd.app.data.Message;

/** Created by Xyzzy on 2017-02-24. */

/* POJO wrapper around the UI corresponding to a room */
public interface RoomUI {

    /* The name of the room */
    String getRoomName();

    /* Display the given messages
     *
     * If a message in the list is already visible (as determined by the message ID), it is to be replaced.
     */
    void showMessages(List<Message> messages);

    /* Update the nickname list with the given session ID-name pairs
     *
     * Changed nicknames are to be replaced. Empty nicknames are not to be shown.
     */
    void showNicks(Map<String, String> nicknames);

    /* Remove the nickname list entries with the given session ID-s */
    void removeNicks(List<String> sessions);

}
