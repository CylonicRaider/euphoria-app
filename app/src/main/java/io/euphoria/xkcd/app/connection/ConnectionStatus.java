package io.euphoria.xkcd.app.connection;

/** Created by Xyzzy on 2017-03-22. */

public enum ConnectionStatus {
    /* Finally disconnected */
    DISCONNECTED(false, false),

    /* Initial connect */
    CONNECTING(false, true),

    /* Temporarily disconnected, trying to get in again */
    RECONNECTING(false, true),

    /* All fine */
    CONNECTED(true, true);

    private final boolean connected;
    private final boolean willConnect;

    private ConnectionStatus(boolean connected, boolean willConnect) {
        this.connected = connected;
        this.willConnect = willConnect;
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean isWillConnect() {
        return willConnect;
    }
}
