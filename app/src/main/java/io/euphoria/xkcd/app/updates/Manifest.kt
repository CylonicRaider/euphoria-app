package io.euphoria.xkcd.app.updates

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.MalformedURLException
import java.net.URL
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

/** Created by Xyzzy on 2020-07-10.  */ // TODO: Create relative URLs when formatting?
class Manifest constructor(val baseURL: URL, obj: JSONObject) {
    class MappingException : Exception {
        constructor() : super()
        constructor(message: String?) : super(message)
        constructor(message: String?, cause: Throwable?) : super(message, cause)
        constructor(cause: Throwable?) : super(cause)
    }

    inner class Release constructor(obj: JSONObject) {
        val version: Version
        val name: String
        val icon: URL
        var date: Long = 0
        var fileURL: URL
        var fileHash: String

        @Throws(JSONException::class)
        fun toJSONObject(): JSONObject {
            val ret: JSONObject = JSONObject()
            ret.put("version", version.toString())
            ret.put("name", name)
            ret.put("icon", icon.toString())
            ret.put("date", formatDate(date))
            ret.put("file", fileURL.toString())
            ret.put("fileHash", fileHash)
            return ret
        }

        init {
            try {
                this.version =
                    Version.parseVersion(obj.getString("version"))
                this.name = obj.getString("name")
                this.icon = URL(baseURL, obj.getString("icon"))
                date =
                    parseDate(obj.getString("date"))
                fileURL = URL(baseURL, obj.getString("file"))
                fileHash = obj.getString("sha256")
            } catch (exc: JSONException) {
                throw MappingException(exc)
            } catch (exc: MalformedURLException) {
                throw MappingException(exc)
            } catch (exc: ParseException) {
                throw MappingException(exc)
            }
        }
    }

    val name: String
    val version: Version
    val icon: URL
    val releases: List<Release>

    @Throws(JSONException::class, MappingException::class)
    private fun extractReleases(array: JSONArray): List<Release> {
        val ret: MutableList<Release> = ArrayList()
        for (i in 0 until array.length()) {
            ret.add(Release(array.getJSONObject(i)))
        }
        return Collections.unmodifiableList(ret)
    }

    val latestRelease: Release?
        get() {
            if (releases.isEmpty()) return null
            return Collections.max(
                releases,
                RELEASE_COMPARATOR
            )
        }

    @Throws(JSONException::class)
    fun toJSONObject(): JSONObject {
        val ret: JSONObject = JSONObject()
        ret.put("name", name)
        ret.put("version", version.toString())
        ret.put("icon", icon.toString())
        val releases: JSONArray = JSONArray()
        for (r: Release in this.releases) {
            releases.put(r.toJSONObject())
        }
        ret.put("releases", releases)
        return ret
    }

    companion object {
        val RELEASE_COMPARATOR: Comparator<Release> =
            object : Comparator<Release> {
                override fun compare(r1: Release, r2: Release): Int {
                    return r1.version.compareTo(r2.version)
                }
            }
        private val DATE_FORMAT: SimpleDateFormat = SimpleDateFormat(
            "yyyy-MM-DD HH:mm:ss.SSS Z",
            Locale.ROOT
        )

        @Throws(ParseException::class)
        fun parseDate(text: String): Long {
            // On at least one device, SimpleDateFormat chokes on the final "Z"; substitute it with something equivalent.
            synchronized(
                DATE_FORMAT
            ) {
                return DATE_FORMAT.parse(text.replace("\\s+Z$".toRegex(), " +00:00"))!!
                    .time
            }
        }

        fun formatDate(value: Long): String {
            synchronized(
                DATE_FORMAT
            ) { return DATE_FORMAT.format(value) }
        }
    }

    init {
        try {
            name = obj.getString("name")
            version =
                Version.Companion.parseVersion(obj.getString("version"))
            icon = URL(baseURL, obj.getString("icon"))
            releases = extractReleases(obj.getJSONArray("releases"))
        } catch (exc: JSONException) {
            throw MappingException(exc)
        } catch (exc: MalformedURLException) {
            throw MappingException(exc)
        }
    }
}
