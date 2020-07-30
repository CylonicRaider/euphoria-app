package io.euphoria.xkcd.app

import android.net.Uri
import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL

/** Created by Xyzzy on 2020-07-22.  */
object URLs {
    /* Loosened variations of the patterns in backend/handlers.go as of 097b7da2e0b23e9c5828c0e4831a3de660bb5302.
     * They are lowercase-only indeed. */
    private val ROOM_FRAG_RE =
        Regex("[a-z0-9:]*")
    private val ROOM_NAME_RE =
        Regex("(?:[a-z]+:)?[a-z0-9]+")
    private val ROOM_PATH_RE =
        Regex("/room/(${ROOM_NAME_RE.pattern})/?")

    /**
     * Convert the given Android `Uri` object to a standard library `URL` object.
     *
     * @param uri The `Uri` to convert
     * @return An equivalent `URL`
     */
    fun toURL(uri: Uri?): URL {
        try {
            return URL(uri.toString())
        } catch (exc: MalformedURLException) {
            throw RuntimeException("Invalid URL?!", exc)
        }
    }

    /**
     * Convert the given Android `Uri` object to a standard library `URI` object.
     *
     * @param uri The `Uri` to convert
     * @return An equivalent `URI`
     */
    fun toURI(uri: Uri?): URI {
        try {
            return URI(uri.toString())
        } catch (exc: URISyntaxException) {
            throw RuntimeException("Invalid URI?!", exc)
        }
    }

    /**
     * Convert the given standard library `URL` to an Android `Uri` object.
     *
     * @param url The `URL` to convert
     * @return An equivalent `Uri`
     */
    fun toUri(url: URL?): Uri {
        return Uri.parse(url.toString())
    }

    /**
     * Convert the given standard library `URI` to an Android `Uri` object.
     *
     * @param uri The `URI` to convert
     * @return An equivalent `Uri`
     */
    fun toUri(uri: URI): Uri {
        return Uri.parse(uri.toString())
    }

    /**
     * Test whether the given URI denotes a valid Euphoria room.
     *
     * @param uri The URI to test
     * @return The test result
     */
    fun isValidRoomUri(uri: Uri?): Boolean {
        return uri?.let {
            uri.scheme?.matches(Regex("https?")) ?: false
                    && uri.authority.equals("euphoria.io", ignoreCase = true)
                    && uri.path?.matches(ROOM_PATH_RE) ?: false
        } ?: false
        // Query strings and fragment identifiers are allowed.
    }

    /**
     * Test whether the given characters are a potentially valid component of a room name.
     * Useful for, e.g., pre-filtering text fields.
     *
     * @param chars Characters to test
     * @return The test result
     */
    fun isValidRoomNameFragment(chars: String?): Boolean {
        return chars?.matches(ROOM_FRAG_RE) ?: false
    }

    /**
     * Test whether the given string is a valid Euphoria room name.
     *
     * @param name The string to test
     * @return The test result
     */
    fun isValidRoomName(name: String?): Boolean {
        return name?.matches(ROOM_NAME_RE) ?: false
    }

    /**
     * Retrieve the room name from the given URI.
     *
     * @param uri The URI to probe
     * @return The room name, or `null` if the room name cannot be isolated
     */
    fun getRoomName(uri: Uri): String? {
        val path = uri.path ?: return null
        return ROOM_PATH_RE.find(path)?.let { it.groupValues[1] }
    }

    /**
     * Retrieve WebSocket URL to access the API of a given room.
     *
     * @param roomName The name of the room
     * @return The URL (as a `Uri`)
     * @throws IllegalArgumentException If `roomName` is not valid
     */
    @Throws(IllegalArgumentException::class)
    fun getRoomEndpoint(roomName: String?): Uri {
        if (!isValidRoomName(roomName)) throw IllegalArgumentException("Not a valid room name: $roomName")
        // TODO configure in build script
        return Uri.parse("wss://euphoria.io/room/$roomName/ws?h=1")
    }// TODO configure in build script

    /**
     * Retrieve the URI of the update checker manifest.
     *
     * @return Where the update checker manifest is located
     */
    val updateManifest: Uri
        get() {
            // TODO configure in build script
            return Uri.parse("https://euphoria.leet.nu/app/index.json")
        }
}
