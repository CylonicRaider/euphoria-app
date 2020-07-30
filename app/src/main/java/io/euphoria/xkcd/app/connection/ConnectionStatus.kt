package io.euphoria.xkcd.app.connection

/** Created by Xyzzy on 2017-03-22.  */
enum class ConnectionStatus(
    val isConnected: Boolean,
    val isWillConnect: Boolean
) {
    /* Finally disconnected */
    DISCONNECTED(false, false),  /* Initial connect */
    CONNECTING(false, true),  /* Temporarily disconnected, trying to get in again */
    RECONNECTING(false, true),  /* All fine */
    CONNECTED(true, true);

}
