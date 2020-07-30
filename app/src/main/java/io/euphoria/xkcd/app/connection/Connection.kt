package io.euphoria.xkcd.app.connection

/** Created by Xyzzy on 2017-02-24.  */ /* A connection to the Euphoria backend */
open interface Connection {
    /* The name of the room of this connection */
    val roomName: String?

    /* Close the connection
     *
     * After this, the object should be considered unusable.
     */
    fun close()

    /* Request changing one's nickname
     *
     * @param name The nickname to change to.
     * @return The sequence ID of the message sent.
     */
    fun setNick(name: String?): Int

    /* Post a message
     *
     * @param text The text of the message.
     * @param parent The parent of the message, or null for a new thread.
     * @return The sequence ID of the message sent.
     */
    fun postMessage(text: String?, parent: String?): Int

    /* Request room logs
     *
     * @param before The last message ID to return.
     * @param count The amount of messages to retrieve. The backend imposes a maximum value.
     * @return The sequence ID of the message sent.
     */
    fun requestLogs(before: String?, count: Int): Int

    /* The current connection status as an enum value */
    val status: ConnectionStatus

    /* Add an event listener */
    fun addEventListener(l: ConnectionListener)

    /* Remove an event listener */
    fun removeEventListener(l: ConnectionListener?)
}
