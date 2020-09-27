package io.euphoria.xkcd.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import java.net.HttpCookie;
import java.util.List;

public class Settings {

    private static final String KEY_PRIVATE_MODE = "private_mode";
    private static final String KEY_SESSION_COOKIE = "session_cookie";

    private final SharedPreferences preferences;

    public Settings(Context context) {
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    /**
     * Return whether the "private mode" setting is enabled.
     *
     * In private mode, the session cookie (see {@link #getSessionCookie()}) should be neither read nor written.
     *
     * @return {@code true} in private mode, {@code false} otherwise.
     */
    public boolean isInPrivateMode() {
        return preferences.getBoolean(KEY_PRIVATE_MODE, false);
    }

    /**
     * Retrieve the stored session cookie, if one was stored previously.
     *
     * This reads the cookie unconditionally; also check {@link #isInPrivateMode()} for whether a stored cookie should
     * be used at all.
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
