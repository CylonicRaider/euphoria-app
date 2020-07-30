package io.euphoria.xkcd.app.impl.connection

import android.os.Handler
import android.os.Looper
import io.euphoria.xkcd.app.connection.Connection
import io.euphoria.xkcd.app.connection.ConnectionManager
import java.util.*
import kotlin.collections.HashMap

/** Created by Xyzzy on 2017-02-24.  */ /* Implementation of ConnectionManager */
class ConnectionManagerImpl : ConnectionManager {
    private val handler: Handler = Handler(Looper.getMainLooper())
    private val connections: MutableMap<String, ConnectionImpl> = HashMap()

    @Synchronized
    override fun getConnection(roomName: String): Connection? {
        return connections[roomName]
    }

    @Synchronized
    override fun connect(roomName: String): Connection {
        var conn: ConnectionImpl? = connections[roomName]
        if (conn == null) {
            conn = ConnectionImpl(this, roomName)
            conn.connect()
            connections[roomName] = conn
        }
        return conn
    }

    @Synchronized
    fun remove(conn: ConnectionImpl) {
        connections.remove(conn.roomName)
    }

    @Synchronized
    override fun hasConnections(): Boolean {
        return connections.isNotEmpty()
    }

    @Synchronized
    override fun shutdown() {
        for (c: ConnectionImpl in connections.values) {
            c.close()
        }
    }

    fun invokeLater(cb: Runnable) {
        handler.post(cb)
    }

    fun invokeLater(cb: Runnable, delay: Long) {
        handler.postDelayed(cb, delay)
    }
}
