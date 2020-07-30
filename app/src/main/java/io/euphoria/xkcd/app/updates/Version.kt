package io.euphoria.xkcd.app.updates

import io.euphoria.xkcd.app.BuildConfig
import java.util.regex.Pattern

/** Created by Xyzzy on 2020-07-08.  */
class Version constructor(vararg fields: Int) :
    Comparable<Version?> {
    private val fields: IntArray = fields.clone()
    override fun equals(other: Any?): Boolean {
        return ((other is Version) && (compareTo(other as Version?) == 0))
    }

    override fun hashCode(): Int {
        var hash: Int = 0
        for (f: Int in fields) {
            hash = hash * 31 + f
        }
        return hash
    }

    override fun toString(): String {
        val sb: StringBuilder = StringBuilder()
        var first: Boolean = true
        for (f: Int in fields) {
            if (first) {
                first = false
            } else {
                sb.append('.')
            }
            sb.append(f)
        }
        return sb.toString()
    }

    override fun compareTo(other: Version?): Int {
        val fc: Int = fieldCount
        val ofc: Int = other!!.fieldCount
        var i: Int = 0
        while (true) {
            if (i == fc) {
                if (i == ofc) return 0
                return -1
            } else if (i == ofc) {
                return 1
            } else if (getField(i) != other.getField(i)) {
                return getField(i) - other.getField(i)
            }
            i++
        }
    }

    fun getFields(): IntArray {
        return fields.clone()
    }

    val fieldCount: Int
        get() {
            return fields.size
        }

    fun getField(index: Int): Int {
        return fields.get(index)
    }

    companion object {
        private val VALID_VERSION_STRING =
            Regex("^(?:0|[1-9][0-9]*)(\\.(?:0|[1-9][0-9]*))*$")

        fun parseVersion(text: String): Version {
            if (!(VALID_VERSION_STRING matches text)) throw IllegalArgumentException("Invalid version string $text")
            val rawFields: Array<String> = text.split("\\.".toRegex()).toTypedArray()
            val fields = IntArray(rawFields.size)
            for (i in rawFields.indices) {
                fields[i] = rawFields[i].toInt()
            }
            return Version(*fields)
        }

        val currentAppVersion: Version
            get() {
                return parseVersion(BuildConfig.VERSION_NAME)
            }
    }

}
