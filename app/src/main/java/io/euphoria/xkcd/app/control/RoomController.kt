package io.euphoria.xkcd.app.control

import android.content.Context
import android.os.Handler
import io.euphoria.xkcd.app.connection.Connection
import io.euphoria.xkcd.app.connection.ConnectionListener
import io.euphoria.xkcd.app.connection.ConnectionManager
import io.euphoria.xkcd.app.connection.ConnectionStatus
import io.euphoria.xkcd.app.connection.event.*
import io.euphoria.xkcd.app.ui.RoomUI
import io.euphoria.xkcd.app.ui.RoomUIManager
import io.euphoria.xkcd.app.ui.UIListener
import io.euphoria.xkcd.app.ui.event.*
import java.util.*

/** Created by Xyzzy on 2017-03-19.  */
class RoomController constructor(
    val context: Context,
    uiManager: RoomUIManager,
    connManager: ConnectionManager
) {
    private val handler: Handler
    val roomUIManager: RoomUIManager
    val connectionManager: ConnectionManager
    private val openRooms: MutableSet<String>

    fun openRoom(roomName: String) {
        val conn = connectionManager.connect(roomName)
        val ui = roomUIManager.getRoomUI(roomName)
        if (openRooms.add(roomName)) link(conn, ui)
    }

    fun closeRoom(roomName: String) {
        connectionManager.getConnection(roomName)?.close()
        roomUIManager.getRoomUI(roomName).close()
        openRooms.remove(roomName)
    }

    fun shutdown() {
        connectionManager.shutdown()
        val rooms: List<String> =
            ArrayList(openRooms)
        for (roomName: String in rooms) {
            closeRoom(roomName)
        }
    }

    fun invokeLater(cb: Runnable) {
        handler.post(cb)
    }

    fun invokeLater(cb: Runnable, delay: Long) {
        handler.postDelayed(cb, delay)
    }

    protected fun link(
        conn: Connection,
        ui: RoomUI
    ) {
        ui.setConnectionStatus(ConnectionStatus.CONNECTING)
        conn.addEventListener(object : ConnectionListener {
            override fun onOpen(evt: OpenEvent?) {
                invokeLater(Runnable { ui.setConnectionStatus(ConnectionStatus.CONNECTED) })
            }

            override fun onIdentity(evt: IdentityEvent) {
                invokeLater(Runnable { ui.showNicks(listOf(evt.identity)) })
            }

            override fun onNickChange(evt: NickChangeEvent) {
                invokeLater(Runnable { ui.showNicks(listOf(evt.session)) })
            }

            override fun onMessage(evt: MessageEvent) {
                invokeLater(Runnable { ui.showMessages(listOf(evt.message)) })
            }

            override fun onPresenceChange(evt: PresenceChangeEvent) {
                invokeLater(Runnable {
                    if (evt.isPresent) {
                        ui.showNicks(evt.sessions)
                    } else {
                        ui.removeNicks(evt.sessions)
                    }
                })
            }

            override fun onLogEvent(evt: LogEvent) {
                invokeLater(Runnable { ui.showMessages(evt.messages) })
            }

            override fun onClose(evt: CloseEvent) {
                invokeLater(Runnable { ui.setConnectionStatus(if (evt.isFinal) ConnectionStatus.DISCONNECTED else ConnectionStatus.RECONNECTING) })
            }
        })
        ui.addEventListener(object : UIListener {
            override fun onNewNick(evt: NewNickEvent) {
                conn.setNick(evt.newNick)
            }

            override fun onMessageSend(evt: MessageSendEvent) {
                conn.postMessage(evt.text, evt.parent)
            }

            override fun onLogRequest(evt: LogRequestEvent) {
                conn.requestLogs(
                    evt.before,
                    DEFAULT_LOG_REQUEST_AMOUNT
                )
            }

            override fun onRoomSwitch(evt: RoomSwitchEvent) {
                openRoom(evt.roomName)
            }

            override fun onClose(evt: UICloseEvent) {
                evt.roomUI?.roomName?.let { closeRoom(it) }
            }
        })
    }

    companion object {
        const val DEFAULT_LOG_REQUEST_AMOUNT: Int = 50
    }

    init {
        handler = Handler(context.mainLooper)
        roomUIManager = uiManager
        connectionManager = connManager
        openRooms = HashSet()
    }
}
