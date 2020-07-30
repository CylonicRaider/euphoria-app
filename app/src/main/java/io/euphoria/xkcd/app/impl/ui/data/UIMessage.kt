package io.euphoria.xkcd.app.impl.ui.data

import android.os.Parcel
import io.euphoria.xkcd.app.data.Message
import io.euphoria.xkcd.app.impl.ui.UIUtils
import java.util.*

/** Created by Xyzzy on 2020-03-19.  */
class UIMessage {
    val id: String
    val parent: String?
    val timestamp: Long
    val senderAgent: String
    val senderName: String
    val content: String
    val isTruncated: Boolean

    constructor(source: Message) {
        id = source.id
        parent = source.parent
        timestamp = source.timestamp
        senderAgent = source.sender.agentID
        senderName = source.sender.name!!
        content = source.content
        isTruncated = source.isTruncated
    }

    constructor(
        `in`: Parcel,
        id: String,
        parent: String?,
        truncated: Boolean
    ) {
        this.id = id
        this.parent = parent
        timestamp = `in`.readLong()
        senderAgent = `in`.readString()!!
        senderName = `in`.readString()!!
        content = `in`.readString()!!
        isTruncated = truncated
    }

    override fun hashCode(): Int {
        return id.hashCode() xor (UIUtils.hashCodeOrNull(parent) shl 4) xor longHashCode(
            timestamp
        ) xor (
                UIUtils.hashCodeOrNull(senderAgent) shl 4) xor (UIUtils.hashCodeOrNull(senderName) shl 8) xor
                UIUtils.hashCodeOrNull(content) xor booleanHashCode(isTruncated)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is UIMessage) return false
        val mo = other
        return id == mo.id &&
                UIUtils.equalsOrNull(parent, mo.parent) && timestamp == mo.timestamp &&
                UIUtils.equalsOrNull(senderAgent, mo.senderAgent) &&
                UIUtils.equalsOrNull(senderName, mo.senderName) &&
                UIUtils.equalsOrNull(content, mo.content) && isTruncated == mo.isTruncated
    }

    override fun toString(): String {
        return String.format(
            (null as Locale?)!!,
            "%s@%h[id=%s,parent=%s,timestamp=%s,sender=[agent=%s,name=%s],content=%s,truncated=%s]",
            javaClass.simpleName,
            this,
            id,
            parent,
            timestamp,
            senderAgent,
            senderName,
            content,
            isTruncated
        )
    }

    fun writeToParcel(out: Parcel) {
        out.writeLong(timestamp)
        out.writeString(senderAgent)
        out.writeString(senderName)
        out.writeString(content)
    }

    companion object {
        private fun longHashCode(n: Long): Int {
            return (n ushr 32).toInt() xor n.toInt()
        }

        private fun booleanHashCode(b: Boolean): Int {
            return if (b) 1231 else 1237
        }
    }
}
