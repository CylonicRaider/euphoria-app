package io.euphoria.xkcd.app.impl.connection;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.net.HttpCookie;

import io.euphoria.xkcd.app.Settings;
import io.euphoria.xkcd.app.connection.SessionCookieStore;

public class SessionCookieStoreImpl implements SessionCookieStore {

    private final Settings settings;

    public SessionCookieStoreImpl(@NonNull Settings settings) {
        this.settings = settings;
    }

    @Nullable
    public HttpCookie getSessionCookie(@NonNull String roomName) {
        return settings.shouldContinuePrevSession() ? settings.getSessionCookie() : null;
    }

    public void putSessionCookie(@NonNull String roomName, @NonNull HttpCookie newSessionCookie) {
        settings.setSessionCookie(newSessionCookie);
    }

}
