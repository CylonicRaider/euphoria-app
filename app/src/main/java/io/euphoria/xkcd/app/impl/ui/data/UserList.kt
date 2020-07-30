package io.euphoria.xkcd.app.impl.ui.data

import android.os.Parcel
import android.os.Parcelable
import io.euphoria.xkcd.app.data.SessionView
import java.util.*

/** Created by Xyzzy on 2020-03-29.  */
class UserList() : Parcelable {
    class UIUser : Comparable<UIUser?> {
        val sessionID: String
        val agentID: String
        var nickname: String?
            private set

        constructor(sessionID: String, agentID: String, nickname: String?) {
            this.sessionID = sessionID
            this.agentID = agentID
            this.nickname = nickname
        }

        constructor(`in`: Parcel) {
            sessionID = `in`.readString()!!
            agentID = `in`.readString()!!
            nickname = `in`.readString()
        }

        fun writeToParcel(dest: Parcel) {
            dest.writeString(sessionID)
            dest.writeString(agentID)
            dest.writeString(nickname)
        }

        override fun hashCode(): Int {
            return agentID.hashCode() shl 4 xor nickname.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            return other is UIUser && compareTo(other as UIUser?) == 0
        }

        override fun toString(): String {
            return String.format(
                (null as Locale?)!!, "%s@%h[%s|%s/%s]", javaClass.simpleName, this,
                nickname, agentID, sessionID
            )
        }

        override fun compareTo(other: UIUser?): Int {
            val res = nickname!!.compareTo(other!!.nickname!!)
            return if (res != 0) res else agentID.compareTo(other.agentID)
        }

        fun setNickname(nickname: String) {
            this.nickname = nickname
        }

        fun updateFrom(other: UIUser) {
            nickname = other.nickname
        }
    }

    private val allUsers: MutableMap<String, UIUser>
    private val displayed: MutableList<UIUser>
    var listener: DisplayListener?
        private set

    protected constructor(`in`: Parcel) : this() {
        val length = `in`.readInt()
        for (i in 0 until length) {
            add(UIUser(`in`))
        }
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
        out.writeInt(allUsers.size)
        for (item in allUsers.values) {
            item.writeToParcel(out)
        }
    }

    fun setDisplayListener(listener: DisplayListener?) {
        var listener = listener
        if (listener == null) listener = DisplayListenerAdapter.Companion.NULL
        this.listener = listener
    }

    fun size(): Int {
        return displayed.size
    }

    operator fun get(index: Int): UIUser? {
        return displayed[index]
    }

    fun has(sid: String?): Boolean {
        return allUsers.containsKey(sid)
    }

    operator fun get(sid: String?): UIUser? {
        return allUsers[sid]
    }

    fun add(usr: UIUser): UIUser =
        get(usr.sessionID)?.also { processUpdate(it, usr) } ?: usr.also { processInsert(usr) }

    fun add(sess: SessionView): UIUser = add(UIUser(sess.sessionID, sess.agentID, sess.name))

    fun setNick(usr: UIUser, newName: String) {
        val existing = get(usr.sessionID)
            ?: throw IllegalStateException("Trying to rename non-existent user $usr")
        if (existing.nickname == newName) return
        processRename(usr, newName)
    }

    fun remove(usr: UIUser): UIUser? {
        val existing = get(usr.sessionID) ?: return null
        processRemove(existing)
        return existing
    }

    fun addAll(users: List<SessionView>) {
        for (sv in users) add(sv)
    }

    fun removeAll(users: List<SessionView>) {
        for (sv in users) {
            get(sv.sessionID)?.also { remove(it) }
        }
    }

    protected fun findDisplayIndex(usr: UIUser, visible: Boolean): Int {
        var index = Collections.binarySearch(displayed, usr)
        if (index < 0) index = if (usr.nickname!!.isEmpty() || visible) -1 else -index - 1
        return index
    }

    protected fun processInsert(usr: UIUser) {
        allUsers[usr.sessionID] = usr
        val index = findDisplayIndex(usr, false)
        if (index != -1) {
            displayed.add(index, usr)
            listener!!.notifyItemRangeInserted(index, 1)
        }
    }

    protected fun processUpdate(usr: UIUser, update: UIUser) {
        val oldIndex = findDisplayIndex(usr, true)
        usr.updateFrom(update)
        processMoveCommon(usr, oldIndex)
    }

    protected fun processRename(usr: UIUser, newName: String) {
        val oldIndex = findDisplayIndex(usr, true)
        usr.setNickname(newName)
        processMoveCommon(usr, oldIndex)
    }

    protected fun processMoveCommon(usr: UIUser, oldIndex: Int) {
        if (oldIndex != -1) {
            if ((oldIndex == 0 || get(oldIndex - 1)!!.compareTo(usr) <= 0) &&
                (oldIndex == size() - 1 || get(oldIndex + 1)!!.compareTo(usr) >= 0)
            ) {
                listener!!.notifyItemChanged(oldIndex)
                return
            }
            displayed.removeAt(oldIndex)
        }
        val newIndex = findDisplayIndex(usr, false)
        if (newIndex != -1) displayed.add(newIndex, usr)
        DisplayListenerAdapter.Companion.notifyItemMovedLenient(listener, oldIndex, newIndex)
        if (newIndex != -1) listener!!.notifyItemChanged(newIndex)
    }

    protected fun processRemove(usr: UIUser) {
        allUsers.remove(usr.sessionID)
        val index = findDisplayIndex(usr, true)
        if (index != -1) {
            displayed.removeAt(index)
            listener!!.notifyItemRangeRemoved(index, 1)
        }
    }

    companion object {
        val CREATOR: Parcelable.Creator<UserList> = object : Parcelable.Creator<UserList> {
            override fun createFromParcel(`in`: Parcel): UserList {
                return UserList(`in`)
            }

            override fun newArray(size: Int): Array<UserList?> {
                return arrayOfNulls(size)
            }
        }
    }

    init {
        allUsers = HashMap()
        displayed = ArrayList()
        listener = DisplayListenerAdapter.Companion.NULL
    }
}
