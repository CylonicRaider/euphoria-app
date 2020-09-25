package io.euphoria.xkcd.app.connection;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.net.HttpCookie;

public interface SessionCookieStore {
    /**
     * Get the current stored session cookie for the room with the given name.
     *
     * This method is to be called when new connections are created
     * to determine what cookie to present to the server,
     * or that none should be presented if the returned value is null.
     *
     * @param roomName The name of the room for which a stored session cookie should be retrieved.
     * @return The retrieved session cookie for the given room,
     *         or null if no cookie from a previous session was stored,
     *         or session continuation is disabled.
     */
    @Nullable
    HttpCookie getSessionCookie(@NonNull String roomName);


    /**
     * Update the stored session cookie for the room with the given name.
     *
     * This method is to be called when a connection receives a <code>Set-Cookie</code>-Header
     * during the initial Handshake, which is used by the server to convey a new session cookie.
     *
     * @param roomName The name of the room for which the stored session cookie should be updated.
     */
    void putSessionCookie(@NonNull String roomName, @NonNull HttpCookie newSessionCookie);
}
