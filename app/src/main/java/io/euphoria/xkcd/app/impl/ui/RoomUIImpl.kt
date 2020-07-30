package io.euphoria.xkcd.app.impl.ui

import android.util.Log
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import io.euphoria.xkcd.app.R
import io.euphoria.xkcd.app.connection.ConnectionStatus
import io.euphoria.xkcd.app.data.Message
import io.euphoria.xkcd.app.data.SessionView
import io.euphoria.xkcd.app.impl.ui.data.UIMessage
import io.euphoria.xkcd.app.impl.ui.views.MessageListAdapter
import io.euphoria.xkcd.app.impl.ui.views.UserListAdapter
import io.euphoria.xkcd.app.ui.RoomUI
import io.euphoria.xkcd.app.ui.UIListener
import io.euphoria.xkcd.app.ui.event.*
import java.util.*

open class RoomUIImpl(override val roomName: String) : RoomUI {
    private val listeners: MutableSet<UIListener> =
        LinkedHashSet()
    private var statusDisplay: TextView? = null
    private var messagesAdapter: MessageListAdapter? = null
    private var usersAdapter: UserListAdapter? = null

    override fun show() {
        logNYI("Showing a room")
    }

    override fun close() {
        logNYI("Closing a room")
    }

    override fun setConnectionStatus(status: ConnectionStatus) {
        @ColorRes var color = R.color.status_unknown
        @StringRes var content = R.string.status_unknown
        when (status) {
            ConnectionStatus.DISCONNECTED -> {
                color = R.color.status_disconnected
                content = R.string.status_disconnected
            }
            ConnectionStatus.CONNECTING -> {
                color = R.color.status_connecting
                content = R.string.status_connecting
            }
            ConnectionStatus.RECONNECTING -> {
                color = R.color.status_reconnecting
                content = R.string.status_reconnecting
            }
            ConnectionStatus.CONNECTED -> {
                color = R.color.status_connected
                content = R.string.status_connected
            }
        }
        // This one is called after unlinking; handle that case gracefully.
        if (statusDisplay == null) {
            Log.e("RoomUIImpl", "Lost connection status update: $status")
            return
        }
        statusDisplay!!.setTextColor(UIUtils.getColor(statusDisplay!!.context, color))
        statusDisplay!!.setText(content)
    }

    override fun showMessages(messages: List<Message>) {
        for (m in messages) {
            messagesAdapter!!.add(UIMessage(m))
        }
    }

    override fun showNicks(sessions: List<SessionView>) {
        usersAdapter!!.data.addAll(sessions)
    }

    override fun removeNicks(sessions: List<SessionView>) {
        usersAdapter!!.data.removeAll(sessions)
    }

    /**
     * Adds an UIListener to the RoomUIImpl.
     * If the listener object is already registered,
     * the method will not register it again.
     *
     * @param l Listener to add
     */
    override fun addEventListener(l: UIListener) {
        listeners.add(l)
    }

    /**
     * Removes an UIListener from the RoomUIImpl.
     * If the listener object is not registered,
     * the method will change nothing.
     *
     * @param l Listener to remove
     */
    override fun removeEventListener(l: UIListener) {
        listeners.remove(l)
    }

    fun link(
        status: TextView?,
        messages: MessageListAdapter?,
        users: UserListAdapter?
    ) {
        statusDisplay = status
        messagesAdapter = messages
        usersAdapter = users
    }

    fun unlink(
        status: TextView,
        messages: MessageListAdapter,
        users: UserListAdapter
    ) {
        if (statusDisplay === status) statusDisplay = null
        if (messagesAdapter === messages) messagesAdapter = null
        if (usersAdapter === users) usersAdapter = null
    }

    fun submitEvent(evt: UIEvent) {
        for (l in listeners) {
            if (evt is NewNickEvent) {
                l.onNewNick(evt)
            } else if (evt is MessageSendEvent) {
                l.onMessageSend(evt)
            } else if (evt is LogRequestEvent) {
                l.onLogRequest(evt)
            } else if (evt is RoomSwitchEvent) {
                l.onRoomSwitch(evt)
            } else if (evt is UICloseEvent) {
                l.onClose(evt)
            } else {
                Log.e("RoomUIImpl", "Unknown UI event $evt; dropping.")
            }
        }
    }

    companion object {
        private fun logNYI(detail: String) {
            Log.e("RoomUIImpl", "$detail is not yet implemented...")
        }
    }

}
