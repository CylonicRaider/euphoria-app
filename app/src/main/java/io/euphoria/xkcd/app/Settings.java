package io.euphoria.xkcd.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import java.net.HttpCookie;
import java.util.List;

public class Settings {

    private static final String KEY_CONTINUE_PREV_SESSION = "continue_prev_session";
    private static final String KEY_SESSION_COOKIE = "session_cookie";

    private final SharedPreferences preferences;

    public Settings(Context context) {
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    /**
     * Return whether the previous session should be continued.
     *
     * This retrieves the value of a user-configurable setting.
     *
     * @return {@code true} if the previous session should be continued, {@code false} otherwise.
     */
    public boolean shouldContinuePrevSession() {
        return preferences.getBoolean(KEY_CONTINUE_PREV_SESSION, false);
    }

    /**
     * Retrieve the stored session cookie, if one was stored previously.
     *
     * This reads the cookie unconditionally; also check {@link #shouldContinuePrevSession()} for whether a stored
     * cookie should be used at all.
     *
     * @return The retrieved session cookie, or {@code null} if none was stored previously.
     */
    @Nullable
    public HttpCookie getSessionCookie() {
        String cookieStr = preferences.getString(KEY_SESSION_COOKIE, null);
        if (cookieStr == null) return null;
        List<HttpCookie> parsedCookies = HttpCookie.parse(cookieStr);
        if (parsedCookies.size() != 1) return null;
        return parsedCookies.get(0);
    }

    /**
     * Store the current session cookie to be re-used for future sessions.
     *
     * @param sessionCookie The session cookie to store, or {@code null} to clear the currently stored session cookie.
     */
    public void setSessionCookie(@Nullable HttpCookie sessionCookie) {
        SharedPreferences.Editor prefEditor = preferences.edit();
        if (sessionCookie == null) {
            prefEditor.remove(KEY_SESSION_COOKIE);
        } else {
            prefEditor.putString(KEY_SESSION_COOKIE, sessionCookie.toString());
        }
        prefEditor.apply();
    }

}
