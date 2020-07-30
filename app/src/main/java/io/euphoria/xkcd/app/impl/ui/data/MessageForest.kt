package io.euphoria.xkcd.app.impl.ui.data

import android.os.Parcel
import android.os.Parcelable
import io.euphoria.xkcd.app.impl.ui.UIUtils
import io.euphoria.xkcd.app.impl.ui.data.MessageTree
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/** Created by Xyzzy on 2020-03-19.  */
class MessageForest() : Parcelable {
    /* All messages by ID. */
    private val allMessages: MutableMap<String, MessageTree> = HashMap()

    /* The roots (i.e. messages with a parent of null) ordered by ID. */
    private val roots: MutableList<MessageTree> = ArrayList()

    /* Messages whose parent has not been added yet (mapping from parent ID). */
    private val orphans: MutableMap<String, MutableList<MessageTree>> = HashMap()

    /* The list of all visible messages in their proper order. */
    private val displayed: MutableList<MessageTree> = ArrayList()

    /* A listener for structural changes of the display list. */
    private var _listener: DisplayListener = DisplayListenerAdapter.NULL
    val listener get() = _listener

    fun setListener(listener: DisplayListener?) {
        this._listener = listener ?: DisplayListenerAdapter.NULL
    }

    protected constructor(`in`: Parcel) : this() {
        roots.addAll(readGroupFromParcel(`in`))
        while (true) {
            val marker = `in`.readByte()
            if (marker.toInt() == 0) {
                break
            } else require(marker == MessageTree.Companion.PF_IS_A_THING) { "Invalid parcelled data" }
            readGroupFromParcel(`in`)
        }
        for (mt in roots) {
            displayed.addAll(mt.traverseVisibleReplies(true))
        }
    }

    override fun describeContents(): Int {
        return 0 // Nothing to see here.
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
        writeGroupToParcel(out, null, roots)
        for ((key, value) in orphans) {
            out.writeByte(MessageTree.Companion.PF_IS_A_THING)
            writeGroupToParcel(out, key, value)
        }
        out.writeByte(0.toByte())
    }

    private fun addToAllMessagesRecursive(mt: MessageTree) {
        allMessages[mt.id] = mt
        for (r in mt.replies) {
            addToAllMessagesRecursive(r)
        }
    }

    private fun readGroupFromParcel(`in`: Parcel): List<MessageTree> {
        val parent = `in`.readString()
        val ret: MutableList<MessageTree> = ArrayList()
        while (true) {
            val flags = `in`.readByte()
            if (flags.toInt() == 0) break
            val mt = MessageTree(`in`, flags, parent)
            addToAllMessagesRecursive(mt)
            ret.add(mt)
        }
        if (parent != null) {
            orphans[parent] = ret
        }
        return ret
    }

    private fun writeGroupToParcel(
        out: Parcel,
        parent: String?,
        mts: List<MessageTree?>
    ) {
        out.writeString(parent)
        for (mt in mts) {
            mt!!.writeToParcel(out)
        }
        out.writeByte(0.toByte())
    }

    fun size(): Int {
        return displayed.size
    }

    operator fun get(index: Int): MessageTree? {
        return displayed[index]
    }

    fun indexOf(mt: MessageTree): Int {
        return findDisplayIndex(mt, visible = true, markUpdate = false)
    }

    fun has(id: String): Boolean {
        return allMessages.containsKey(id)
    }

    operator fun get(id: String): MessageTree? {
        return allMessages[id]
    }

    fun getParent(mt: MessageTree): MessageTree? {
        return mt.parent?.let { get(it) }
    }

    fun getThreadRoot(mt: MessageTree): MessageTree? {
        var mt = mt
        while (true) {
            val parentID = mt.parent ?: return mt
            mt = get(parentID) ?: return null
        }
    }

    fun getReply(mt: MessageTree, index: Int): MessageTree? {
        var index = index
        val replies = mt.replies
        if (index < 0) index += replies.size
        return if (index < 0 || index >= replies.size) null else replies[index]
    }

    fun getSibling(mt: MessageTree, offset: Int): MessageTree? {
        val container: List<MessageTree> = when {
            mt.parent == null -> roots
            !has(mt.parent!!) -> getOrphanList(mt.parent!!)
            else -> getParent(mt)!!.replies
        }

        var index = Collections.binarySearch(container, mt)
        if (index < 0) throw NoSuchElementException("Trying to get sibling of non-linked-in MessageTree $mt")
        index += offset
        return if (index < 0 || index >= container.size) null else container[index]
    }

    protected fun getOrphanList(parentID: String): MutableList<MessageTree> {
        var ret = orphans[parentID]
        if (ret == null) {
            ret = ArrayList()
            orphans[parentID] = ret
        }
        return ret
    }

    protected fun findRootDisplayIndex(mt: MessageTree, visible: Boolean): Int {
        var index = 0
        while (index < displayed.size) {
            val cur = displayed[index]
            if (cur >= mt) return if (visible && cur != mt) -1 else index
            index += 1 + cur.countVisibleReplies()
        }
        return if (visible) -1 else index
    }

    protected fun findDisplayIndex(
        mt: MessageTree,
        visible: Boolean,
        markUpdate: Boolean
    ): Int {
        if (mt.parent == null) return findRootDisplayIndex(mt, visible)
        val parent = getParent(mt)
        if (parent == null || parent.isCollapsed) return -1
        var index = findDisplayIndex(parent, false, markUpdate)
        if (index == -1) return -1
        if (markUpdate) _listener.notifyItemChanged(index)
        index++
        for (sib in parent.replies) {
            if (sib >= mt) return if (visible && sib != mt) -1 else index
            index += 1 + sib.countVisibleReplies()
        }
        return if (visible) -1 else index
    }

    fun clear() {
        val oldLength = displayed.size
        allMessages.clear()
        roots.clear()
        orphans.clear()
        displayed.clear()
        _listener.notifyItemRangeRemoved(0, oldLength)
    }

    fun add(mt: MessageTree): MessageTree? {
        val existing = allMessages[mt.id]
        return if (existing == null) {
            processInsert(mt)
            mt
        } else if (!UIUtils.equalsOrNull(mt.message, existing.message)) {
            processReplace(mt)
            mt
        } else {
            existing
        }
    }

    fun add(msg: UIMessage): MessageTree {
        val existing = allMessages[msg.id]
        return if (existing == null) {
            val mt = MessageTree(msg)
            processInsert(mt)
            mt
        } else if (!UIUtils.equalsOrNull(msg, existing.message)) {
            existing.setMessage(msg)
            processReplace(existing)
            existing
        } else {
            existing
        }
    }

    fun setCollapsed(mt: MessageTree, collapsed: Boolean) {
        if (collapsed == mt.isCollapsed) return
        processCollapse(mt, collapsed)
    }

    fun toggleCollapsed(mt: MessageTree) {
        processCollapse(mt, !mt.isCollapsed)
    }

    fun tryEnsureVisible(mt: MessageTree, expand: Boolean): Boolean {
        val ret: Boolean
        val parent = getParent(mt)
        ret = if (mt.parent == null) {
            // Roots are always visible.
            true
        } else if (parent == null) {
            // Orphans cannot be made visible.
            false
        } else {
            // Otherwise, we go to the parent.
            tryEnsureVisible(parent, true)
        }
        if (ret && expand) setCollapsed(mt, false)
        return ret
    }

    fun move(
        mt: MessageTree,
        newParent: MessageTree?,
        ensureVisible: Boolean
    ) {
        if (ensureVisible && newParent != null) tryEnsureVisible(newParent, true)
        val newParentID = newParent?.id
        if (UIUtils.equalsOrNull(mt.parent, newParentID)) return
        processMove(mt, newParent)
    }

    fun remove(mt: MessageTree, recursive: Boolean): MessageTree? {
        val existing = allMessages[mt.id] ?: return null
        processRemove(existing, recursive)
        return existing
    }

    protected fun addDisplayRange(
        mt: MessageTree,
        index: Int,
        includeSelf: Boolean
    ) {
        var index = index
        val toAdd = mt.traverseVisibleReplies(includeSelf)
        if (!includeSelf) index++
        displayed.addAll(index, toAdd)
        _listener.notifyItemRangeInserted(index, toAdd.size)
    }

    protected fun removeDisplayRange(
        mt: MessageTree,
        index: Int,
        includeSelf: Boolean
    ) {
        var index = index
        var length = mt.countVisibleReplies()
        if (includeSelf) {
            length++
        } else {
            index++
        }
        displayed.subList(index, index + length).clear()
        _listener.notifyItemRangeRemoved(index, length)
    }

    protected fun processInsert(mt: MessageTree) {
        allMessages[mt.id] = mt
        // Adopt orphans! :)
        val children: List<MessageTree>? = orphans.remove(mt.id)
        if (children != null) mt.addReplies(children)
        // The next steps depend on whether the message has a parent.
        val displayIndex: Int
        val parentID = mt.parent
        if (parentID == null) {
            // If there is no parent, this is a new root.
            UIUtils.insertSorted(roots, mt)
            mt.updateIndent(0)
            displayIndex = findRootDisplayIndex(mt, visible = false)
        } else if (!has(parentID)) {
            // If the parent does not exist, the message is an orphan.
            getOrphanList(parentID).add(mt)
            return
        } else {
            // Otherwise, a parent exists.
            displayIndex = findDisplayIndex(mt, visible = false, markUpdate = true)
            get(parentID)!!.addReply(mt)
            // Updating the display list does, naturally, not happen for invisible messages.
            if (displayIndex == -1) return
        }
        // Finally, splice the message (along with its replies!) into the display list.
        addDisplayRange(mt, displayIndex, includeSelf = true)
    }

    protected fun processReplace(mt: MessageTree) {
        // If an already-existing MessageTree's underlying message has been replaced, we only need to mark it as
        // changed (and do so for all its parents for good measure).
        val index = findDisplayIndex(mt, visible = true, markUpdate = true)
        if (index != -1) _listener.notifyItemChanged(index)
    }

    protected fun processCollapse(mt: MessageTree, collapse: Boolean) {
        // Locate the message, marking its parents for refreshing. If it is invisible, there is little to do.
        val displayIndex = findDisplayIndex(mt, visible = true, markUpdate = true)
        if (displayIndex == -1) {
            mt.isCollapsed = collapse
            return
        }
        // Do not forget to mark the message itself for updating.
        _listener.notifyItemChanged(displayIndex)
        // Now to the main branch.
        if (collapse) {
            removeDisplayRange(mt, displayIndex, includeSelf = false)
            mt.isCollapsed = true
        } else {
            mt.isCollapsed = false
            addDisplayRange(mt, displayIndex, includeSelf = false)
        }
    }

    protected fun processMove(mt: MessageTree, newParent: MessageTree?) {
        // Just in case someone "moves" a nonexistent message.
        if (!allMessages.containsKey(mt.id)) allMessages[mt.id] = mt
        // The sequence of operations is somewhat tricky, in particular w.r.t. ensuring we pass the right indices
        // to the update listener.
        // First, unlink the message from the data structures.
        val oldIndex = findDisplayIndex(mt, visible = true, markUpdate = true)
        if (oldIndex != -1) {
            displayed.removeAt(oldIndex)
        }
        val oldParent = getParent(mt)
        oldParent?.removeReply(mt)
            ?: if (mt.parent == null) {
                UIUtils.removeSorted(roots, mt)
            } else {
                val siblings =
                    orphans[mt.parent!!]
                siblings?.remove(mt)
            }
        // Now, flip the message's parent to the new one.
        mt.parent = newParent?.id
        // Next, re-link the message into the data structures.
        // Here, we cannot use findDisplayIndex()' auto-notification feature as we have not yet notified the listener
        // about the message's move.
        val newIndex = findDisplayIndex(mt, visible = false, markUpdate = false)
        if (newIndex != -1) {
            displayed.add(newIndex, mt)
        }
        if (newParent != null) {
            newParent.addReply(mt)
        } else {
            UIUtils.insertSorted(roots, mt)
            mt.updateIndent(0)
        }
        // Finally, issue listener notifications.
        DisplayListenerAdapter.Companion.notifyItemMovedLenient(_listener, oldIndex, newIndex)
        findDisplayIndex(mt, visible = true, markUpdate = true)
    }

    protected fun processRemove(mt: MessageTree, recursive: Boolean) {
        allMessages.remove(mt.id)
        if (recursive) {
            // If removing recursively, the children need not become orphans, but have to be removed from the ID index
            // instead.
            for (r in mt.traverseVisibleReplies(false)) allMessages.remove(r.id)
        } else {
            // Create orphans! :(
            getOrphanList(mt.id).addAll(mt.replies)
        }
        // Remove the message from the root list.
        if (mt.parent == null) UIUtils.removeSorted(roots, mt)
        // Locate the message in the display list, and mark its parents for updates.
        val displayIndex = findDisplayIndex(mt, true, true)
        if (displayIndex == -1) return
        // Splice the message along with its replies out.
        removeDisplayRange(mt, displayIndex, true)
    }

    companion object {
        val CREATOR: Parcelable.Creator<MessageForest?> =
            object : Parcelable.Creator<MessageForest?> {
                override fun createFromParcel(`in`: Parcel): MessageForest? {
                    return MessageForest(`in`)
                }

                override fun newArray(size: Int): Array<MessageForest?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
