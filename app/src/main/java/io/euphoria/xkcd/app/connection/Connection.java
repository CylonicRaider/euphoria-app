package io.euphoria.xkcd.app.connection;

/** Created by Xyzzy on 2017-02-24. */

/* A connection to the Euphoria backend */
public interface Connection {

    /* The name of the room of this connection */
    String getRoomName();

    /* Close the connection
     *
     * After this, the object should be considered unusable.
     */
    void close();

    /* Request changing one's nickname
     *
     * @param name The nickname to change to.
     * @return The sequence ID of the message sent.
     */
    int setNick(String name);

    /* Post a message
     *
     * @param text The text of the message.
     * @param parent The parent of the message, or null for a new thread.
     * @return The sequence ID of the message sent.
     */
    int postMessage(String text, String parent);

    /* Request room logs
     *
     * @param before The last message ID to return.
     * @param count The amount of messages to retrieve. The backend imposes a maximum value.
     * @return The sequence ID of the message sent.
     */
    int requestLogs(String before, int count);

    /* Add an event listener */
    void addEventListener(ConnectionListener l);

    /* Remove an event listener */
    void removeEventListener(ConnectionListener l);

}
