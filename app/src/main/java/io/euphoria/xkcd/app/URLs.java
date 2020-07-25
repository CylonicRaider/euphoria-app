package io.euphoria.xkcd.app;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Created by Xyzzy on 2020-07-22. */

public class URLs {

    /* Loosened variations of the patterns in backend/handlers.go as of 097b7da2e0b23e9c5828c0e4831a3de660bb5302.
     * They are lowercase-only indeed. */
    private static final Pattern ROOM_FRAG_RE = Pattern.compile("[a-z0-9:]*");
    private static final Pattern ROOM_NAME_RE = Pattern.compile("(?:[a-z]+:)?[a-z0-9]+");
    private static final Pattern ROOM_PATH_RE = Pattern.compile("/room/(" + ROOM_NAME_RE.pattern() + ")/?");

    private URLs() {}

    /**
     * Test whether the given URI denotes a valid Euphoria room.
     *
     * @param uri The URI to test
     * @return The test result
     */
    public static boolean isValidRoomUri(@Nullable Uri uri) {
        return uri != null && Pattern.matches("https?", uri.getScheme()) &&
                "euphoria.io".equalsIgnoreCase(uri.getAuthority()) &&
                uri.getPath() != null && ROOM_PATH_RE.matcher(uri.getPath()).matches();
        // Query strings and fragment identifiers are allowed.
    }

    /**
     * Test whether the given characters are a potentially valid component of a room name.
     * Useful for, e.g., pre-filtering text fields.
     *
     * @param chars Characters to test
     * @return The test result
     */
    public static boolean isValidRoomNameFragment(String chars) {
        return ROOM_FRAG_RE.matcher(chars).matches();
    }

    /**
     * Test whether the given string is a valid Euphoria room name.
     *
     * @param name The string to test
     * @return The test result
     */
    public static boolean isValidRoomName(String name) {
        return ROOM_NAME_RE.matcher(name).matches();
    }

    /**
     * Retrieve the room name from the given URI.
     *
     * @param uri The URI to probe
     * @return The room name, or {@code null} if the room name cannot be isolated
     */
    public static String getRoomName(@NonNull Uri uri) {
        Matcher m = ROOM_PATH_RE.matcher(uri.getPath());
        if (!m.matches()) return null;
        return m.group(1);
    }

    /**
     * Retrieve WebSocket URL to access the API of a given room.
     *
     * @param roomName The name of the room
     * @return The URL (as a {@code Uri})
     * @throws IllegalArgumentException If {@code roomName} is not valid
     */
    public static Uri getRoomEndpoint(String roomName) throws IllegalArgumentException {
        if (!isValidRoomName(roomName))
            throw new IllegalArgumentException("Not a valid room name: " + roomName);
        // TODO configure in build script
        return Uri.parse("wss://euphoria.io/room/" + roomName + "/ws?h=1");
    }

    /**
     * Retrieve the URI of the update checker manifest.
     *
     * @return Where the update checker manifest is located.
     */
    public static Uri getUpdateManifest() {
        // TODO configure in build script
        return Uri.parse("https://euphoria.leet.nu/app/index.json");
    }

}
