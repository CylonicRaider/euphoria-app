package io.euphoria.xkcd.app.connection;

/** Created by Xyzzy on 2017-02-24. */

public interface Connection {

    /** The name of the room of this connection */
    String getRoomName();

    /**
     * Close the connection
     *
     * After this, the object should be considered unusable.
     */
    void close();

    /** Request changing one's nickname */
    void setNick(String name);

    /**
     * Post a message
     *
     * @param text The text of the message.
     * @param parent The parent of the message, or {@code null} for a new thread.
     */
    void postMessage(String text, String parent);

    /**
     * Request room logs
     *
     * @param before The last message ID to return.
     * @param count The amount of messages to retrieve. The backend imposes a maximum value.
     */
    void requestLogs(String before, int count);

}
