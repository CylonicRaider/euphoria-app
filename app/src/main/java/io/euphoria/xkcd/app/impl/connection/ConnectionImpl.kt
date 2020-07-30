package io.euphoria.xkcd.app.impl.connection

import android.util.Log
import io.euphoria.xkcd.app.URLs
import io.euphoria.xkcd.app.connection.Connection
import io.euphoria.xkcd.app.connection.ConnectionListener
import io.euphoria.xkcd.app.connection.ConnectionStatus
import io.euphoria.xkcd.app.connection.event.*
import io.euphoria.xkcd.app.impl.connection.ConnectionImpl
import java.util.*

/** Created by Xyzzy on 2017-04-29.  */
class ConnectionImpl(val parent: ConnectionManagerImpl, override val roomName: String?) :
    Connection {
    private val listeners: MutableList<ConnectionListener> = ArrayList()

    @get:Synchronized
    override var status: ConnectionStatus = ConnectionStatus.CONNECTING
        private set
    private var client: EuphoriaWebSocketClient? = null
    private var seqid = 0

    @Synchronized
    fun connect() {
        // FIXME: Allow specifying a custom URL template.
        client = EuphoriaWebSocketClient(this, URLs.toURI(URLs.getRoomEndpoint(roomName)))
        client!!.connect()
    }

    override fun close() {
        parent.remove(this)
        client!!.close()
        synchronized(this) { status = ConnectionStatus.DISCONNECTED }
    }

    @Synchronized
    fun sequence(): Int {
        return seqid++
    }

    override fun setNick(name: String?): Int {
        return client!!.sendObject("nick", "name", name)
    }

    override fun postMessage(text: String?, parent: String?): Int {
        return client!!.sendObject("send", "content", text, "parent", parent)
    }

    override fun requestLogs(before: String?, count: Int): Int {
        return client!!.sendObject("log", "n", count, "before", before)
    }

    fun submitEvent(evt: ConnectionEvent) {
        var listeners: List<ConnectionListener>
        synchronized(this) { listeners = ArrayList(this.listeners) }
        for (l in listeners) {
            if (evt is OpenEvent) {
                synchronized(this) { status = ConnectionStatus.CONNECTED }
                l.onOpen(evt)
            } else if (evt is IdentityEvent) {
                l.onIdentity(evt)
            } else if (evt is NickChangeEvent) {
                l.onNickChange(evt)
            } else if (evt is MessageEvent) {
                l.onMessage(evt)
            } else if (evt is PresenceChangeEvent) {
                l.onPresenceChange(evt)
            } else if (evt is LogEvent) {
                l.onLogEvent(evt)
            } else if (evt is CloseEvent) {
                synchronized(this) {
                    if (evt.isFinal) {
                        status = ConnectionStatus.DISCONNECTED
                    } else {
                        status = ConnectionStatus.RECONNECTING
                        parent.invokeLater(Runnable {
                            synchronized(
                                this@ConnectionImpl
                            ) { if (status != ConnectionStatus.DISCONNECTED) connect() }
                        }, 1000)
                    }
                }
                l.onClose(evt)
            } else {
                Log.e(
                    "ConnectionImpl",
                    "Unknown connection event $evt; dropping."
                )
            }
        }
    }

    @Synchronized
    override fun addEventListener(l: ConnectionListener) {
        listeners.add(l)
    }

    @Synchronized
    override fun removeEventListener(l: ConnectionListener?) {
        listeners.remove(l)
    }
}
