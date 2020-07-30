package io.euphoria.xkcd.app.impl.ui.data

import android.os.Parcel
import io.euphoria.xkcd.app.hasFlags
import io.euphoria.xkcd.app.impl.ui.UIUtils
import io.euphoria.xkcd.app.withFlags
import java.util.*

/**
 * @author N00bySumairu
 */
class MessageTree : Comparable<MessageTree> {
    private val _replies: MutableList<MessageTree> = ArrayList()
    val replies: List<MessageTree>
        /** Sorted list of the immediate replies of this tree.  */
        get() = Collections.unmodifiableList(_replies)
    /** The ID of this MessageTree (CURSOR_ID for the input bar).  */
    lateinit var id: String
        private set

    private var _parent: String? = null
    /** The id of the parent node of this MessageTree.  */
    var parent: String?
        get() = _parent
        /**
         * Set this MessageTree's parent ID.
         * Only permissible for a MessageTree representing the input bar.
         */
        set(parent) {
            check(message == null) { "Attempting to change parent of message" }
            _parent = parent
        }
    /** The message wrapped by this; null for the input bar.  */
    var message: UIMessage? = null
        private set

    /**
     * A long containing the numerical value of this message's ID.
     * CURSOR_ID is mapped to the special value -1.
     */
    var longID: Long
        private set
    /** The indentation level of this.  */
    var indent = 0
        private set
    /** Whether the *replies* of this are invisible.  */
    /** Set the [.isCollapsed] flag.  */
    var isCollapsed = false

    constructor(m: UIMessage?) {
        message = m
        if (m == null) {
            id = CURSOR_ID
            _parent = null
        } else {
            id = m.id
            _parent = m.parent
        }
        longID = idStringToLong(id)
    }

    constructor(`in`: Parcel, flags: Byte, parent: String?) {
        require(flags.hasFlags(PF_IS_A_THING)) { "Invalid encoded MessageTree flags" }
        val id = `in`.readString()!!
        if (flags.hasFlags(PF_HAS_CONTENT)) {
            message =
                UIMessage(`in`, id, parent, flags.hasFlags(PF_TRUNCATED))
        }
        this.id = id
        this._parent = parent
        longID = idStringToLong(id)
        isCollapsed = flags.hasFlags(PF_COLLAPSED)
        if (flags.hasFlags(PF_HAS_REPLIES)) {
            while (true) {
                val nextFlags = `in`.readByte()
                if (nextFlags.toInt() == 0) break
                addReply(MessageTree(`in`, nextFlags, id))
            }
        }
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is MessageTree && compareTo(other) == 0
    }

    override fun toString(): String {
        return String.format(
            "%s@%x[%s/%s]",
            javaClass.simpleName,
            hashCode(),
            formatID(_parent),
            formatID(id)
        )
    }

    override fun compareTo(other: MessageTree): Int {
        return id.compareTo(other.id)
    }

    /** Change the message wrapped by this MessageTree.  */
    fun setMessage(m: UIMessage) {
        assert(m.id == id) { "Updating MessageTree with unrelated message" }
        message = m
        id = m.id
        _parent = m.parent
        longID = idStringToLong(id)
    }

    fun updateIndent(i: Int) {
        indent = i
        for (t in _replies) t.updateIndent(i + 1)
    }

    /**
     * Add a MessageTree to the replies list.
     * Returns the index into the replies list at which t now resides.
     */
    fun addReply(t: MessageTree): Int {
        checkNotNull(message) { "Input bar cannot have replies" }
        t.updateIndent(indent + 1)
        return UIUtils.insertSorted(_replies, t)
    }

    /** Add every MessageTree in the list as a reply.  */
    fun addReplies(list: Collection<MessageTree>) {
        // TODO replace with bulk insert, sort, and deduplication?
        for (t in list) addReply(t)
    }

    /** Remove a MessageTree from the replies list.  */
    fun removeReply(t: MessageTree) {
        UIUtils.removeSorted(_replies, t)
    }

    /**
     * Return the amount of visible replies to this MessageTree.
     * A MessageTree is visible iff it has no parent or its parent is visible and not collapsed.
     * The message this method is invoked upon is assumed not to have a parent.
     */
    fun countVisibleReplies(): Int {
        // TODO cache this?
        if (isCollapsed) return 0
        var ret = 0
        for (mt in _replies) {
            ret += 1 + mt.countVisibleReplies()
        }
        return ret
    }

    /**
     * Count replies for user display
     * If override is true, this and only this MessageTree is assumed to be visible regardless of its actual state.
     * The input bar does not count; otherwise equivalent to countVisibleReplies().
     */
    fun countVisibleUserReplies(override: Boolean): Int {
        if (!override && isCollapsed) return 0
        var ret = 0
        for (mt in _replies) {
            if (mt.id == CURSOR_ID) continue
            ret += 1 + mt.countVisibleUserReplies(false)
        }
        return ret
    }

    /** Return a list of all the visible replies to this MessageTree.  */
    fun traverseVisibleReplies(includeThis: Boolean): List<MessageTree> {
        val ret: MutableList<MessageTree> = ArrayList()
        if (includeThis) ret.add(this)
        traverseVisibleRepliesInner(ret)
        return ret
    }

    private fun traverseVisibleRepliesInner(drain: MutableList<MessageTree>) {
        if (isCollapsed) return
        for (t in _replies) {
            drain.add(t)
            t.traverseVisibleRepliesInner(drain)
        }
    }

    /** Serialization logic.  */
    fun writeToParcel(out: Parcel) {
        var flags = PF_IS_A_THING
        if (message != null) {
            flags = flags.withFlags(PF_HAS_CONTENT)
            if (message!!.isTruncated) flags = flags.withFlags(PF_TRUNCATED)
        }
        if (_replies.size != 0) {
            flags = flags.withFlags(PF_HAS_REPLIES)
        }
        if (isCollapsed) {
            flags = flags.withFlags(PF_COLLAPSED)
        }
        out.writeByte(flags)
        out.writeString(id)
        if (flags.hasFlags(PF_HAS_CONTENT)) {
            message!!.writeToParcel(out)
        }
        if (flags.hasFlags(PF_HAS_REPLIES)) {
            for (mt in _replies) mt.writeToParcel(out)
            out.writeByte(0.toByte())
        }
    }

    companion object {
        fun idStringToLong(id: String): Long {
            return if (id == CURSOR_ID) -1 else id.toLong(36)
        }

        fun idLongToString(id: Long): String {
            return if (id == -1L) CURSOR_ID else id.toString(36)
        }

        private fun formatID(id: String?): String {
            return if (id == null) "-" else if (id == CURSOR_ID) "~" else id
        }

        const val CURSOR_ID = "\uFFFF"
        const val PF_IS_A_THING: Byte = 0x01
        protected const val PF_HAS_CONTENT: Byte = 0x02
        protected const val PF_HAS_REPLIES: Byte = 0x04
        protected const val PF_TRUNCATED: Byte = 0x08
        protected const val PF_COLLAPSED: Byte = 0x10
    }
}
