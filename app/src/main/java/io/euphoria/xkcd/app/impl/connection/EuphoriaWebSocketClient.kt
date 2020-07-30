package io.euphoria.xkcd.app.impl.connection

import android.util.Log
import io.euphoria.xkcd.app.connection.Connection
import io.euphoria.xkcd.app.connection.event.*
import io.euphoria.xkcd.app.data.Message
import io.euphoria.xkcd.app.data.SessionView
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.URI
import java.util.*

/** Created by Xyzzy on 2017-05-01.  */
class EuphoriaWebSocketClient constructor(val parent: ConnectionImpl, endpoint: URI?) :
    WebSocketClient(endpoint) {
    private class SessionViewImpl constructor(
        override val sessionID: String,
        override val agentID: String,
        override val name: String?,
        override val isStaff: Boolean,
        override val isManager: Boolean,
        override val serverID: String?,
        override val serverEra: String?
    ) : ServerSessionView {

        constructor(base: ServerSessionView, newName: String?) : this(
            base.sessionID, base.agentID, newName, base.isStaff, base.isManager,
            base.serverID, base.serverEra
        )

    }

    private class MessageImpl constructor(
        override val id: String,
        override val parent: String?,
        override val timestamp: Long,
        override val sender: SessionView,
        override val content: String,
        override val isTruncated: Boolean
    ) : Message

    private open inner class EventImpl @JvmOverloads constructor(override val sequenceID: Int = -1) :
        ConnectionEvent {
        override val connection: Connection
            get() {
                return parent
            }

    }

    private inner class OpenEventImpl :
        EventImpl(), OpenEvent

    private inner class IdentityEventImpl constructor(override val identity: ServerSessionView) :
        EventImpl(), IdentityEvent

    private inner class NickChangeEventImpl(
        override val session: ServerSessionView,
        override val oldNick: String?
    ) : EventImpl(), NickChangeEvent {

        override val newNick: String?
            get() {
                return session.name
            }

    }

    private inner class MessageEventImpl(override val message: Message) :
        EventImpl(), MessageEvent

    private inner class PresenceChangeEventImpl(
        val sessionsEx: List<ServerSessionView>,
        override val isPresent: Boolean
    ) : EventImpl(),
        PresenceChangeEvent {
        // HACK: User is supposed to use that read-only anyway.
        override val sessions: List<SessionView> = sessionsEx
    }

    private inner class LogEventImpl(override val messages: List<Message>) :
        EventImpl(), LogEvent

    private inner class CloseEventImpl constructor(override val isFinal: Boolean) :
        EventImpl(), CloseEvent

    private val sessions: MutableMap<String?, ServerSessionView>
    private val sendaheadQueue: Queue<String>
    private var ready: Boolean
    private var closed: Boolean

    override fun onOpen(handshakedata: ServerHandshake) {
        parent.submitEvent(OpenEventImpl())
    }

    private fun dispatchReady() {
        while (true) {
            var item: String?
            synchronized(this) {
                if (ready) return
                item = sendaheadQueue.poll()
                if (item == null) {
                    ready = true
                    return
                }
            }
            send(item)
        }
    }

    override fun onMessage(message: String) {
        val pmessage: JSONObject
        try {
            pmessage = JSONObject(message)
        } catch (e: JSONException) {
            Log.e("EuphoriaWebSocketClient", "Received malformed JSON", e)
            return
        }
        val type: String
        val data: JSONObject
        try {
            type = pmessage.getString("type")
            data = pmessage.optJSONObject("data") ?: JSONObject()
        } catch (e: JSONException) {
            Log.e("EuphoriaWebSocketClient", "Server packet did not contain type", e)
            return
        }
        try {
            when (type) {
                "ping-event" -> send(
                    buildJSONObject(
                        "type",
                        "ping-reply",
                        "data",
                        buildJSONObject(
                            "time",
                            data.getLong("time")
                        )
                    )
                        .toString()
                )
                "hello-event" -> {
                    submitEvent(
                        IdentityEventImpl(
                            parseSessionView(
                                data.getJSONObject("session")
                            )
                        )
                    )
                    dispatchReady()
                }
                "join-event" -> submitEvent(
                    PresenceChangeEventImpl(
                        listOf(parseSessionView(data)),
                        true
                    )
                )
                "network-event" -> {
                    val subtype: String = data.getString("type")
                    if ((subtype == "partition")) {
                        val serverID: String = data.getString("server_id")
                        val serverEra: String = data.getString("server_era")
                        val removed: MutableList<ServerSessionView> =
                            ArrayList()
                        val iter: MutableIterator<ServerSessionView> =
                            sessions.values.iterator()
                        while (iter.hasNext()) {
                            val s: ServerSessionView = iter.next()
                            if ((s.serverID == serverID) && (s.serverEra == serverEra)) {
                                iter.remove()
                                removed.add(s)
                            }
                        }
                        submitEvent(
                            PresenceChangeEventImpl(
                                Collections.unmodifiableList(
                                    removed
                                ), false
                            )
                        )
                    } else {
                        Log.w(
                            "EuphoriaWebSocketClient",
                            "Unknown network-event subtype: $subtype"
                        )
                    }
                    val session: ServerSessionView? = sessions[data.getString("session_id")]
                    if (session == null) {
                        Log.e(
                            "EuphoriaWebSocketClient",
                            ("Dropping nick change of unknown session ID " +
                                    data.getString("session_id") + "!")
                        )
                    } else {
                        submitEvent(
                            NickChangeEventImpl(
                                SessionViewImpl(session, data.getString("to")),
                                session.name
                            )
                        )
                    }
                }
                "nick-event", "nick-reply" -> {
                    val session: ServerSessionView? = sessions[data.getString("session_id")]
                    if (session == null) {
                        Log.e(
                            "EuphoriaWebSocketClient",
                            ("Dropping nick change of unknown session ID " +
                                    data.getString("session_id") + "!")
                        )
                    } else {
                        submitEvent(
                            NickChangeEventImpl(
                                SessionViewImpl(session, data.getString("to")),
                                session.name
                            )
                        )
                    }
                }
                "part-event" -> submitEvent(
                    PresenceChangeEventImpl(
                        listOf(parseSessionView(data)),
                        false
                    )
                )
                "send-event", "send-reply" -> parent.submitEvent(
                    MessageEventImpl(
                        parseMessage(data)
                    )
                )
                "snapshot-event" -> {
                    submitEvent(
                        PresenceChangeEventImpl(
                            parseSessionViewArray(
                                data.getJSONArray(
                                    "listing"
                                )
                            ),
                            true
                        )
                    )
                    submitEvent(
                        LogEventImpl(
                            parseMessageArray(
                                data.getJSONArray("log")
                            )
                        )
                    )
                }
                "get-message-reply" -> submitEvent(
                    LogEventImpl(
                        listOf(
                            parseMessage(
                                data
                            )
                        )
                    )
                )
                "log-reply" -> submitEvent(
                    LogEventImpl(
                        parseMessageArray(
                            data.getJSONArray("log")
                        )
                    )
                )
                "who-reply" -> submitEvent(
                    PresenceChangeEventImpl(
                        parseSessionViewArray(data.getJSONArray("listing")),
                        true
                    )
                )
                else -> Log.i(
                    "EuphoriaWebSocketClient",
                    "Unrecognized packet type $type!"
                )
            }
        } catch (e: JSONException) {
            Log.e(
                "EuphoriaWebSocketClient",
                "$type packet missing required fields",
                e
            )
            Log.e("EuphoriaWebSocketClient", "$data")
        }
    }

    override fun onClose(
        code: Int,
        reason: String,
        remote: Boolean
    ) {
        Log.i(
            "EuphoriaWebSocketClient",
            ("WebSocket connection closed (code " + code + "; reason: \"" + reason +
                    "\"; " + (if (remote) "remote" else "local") + ")")
        )
        doClose(true)
    }

    override fun onError(ex: Exception) {
        Log.e("EuphoriaWebSocketClient", "Error in WebSocket connection:", ex)
        doClose(false)
    }

    fun sendWithQueue(data: String) {
        synchronized(this) {
            if (!ready) {
                sendaheadQueue.add(data)
                return
            }
        }
        send(data)
    }

    fun sendObject(type: String?, vararg data: Any?): Int {
        val seq: Int = parent.sequence()
        try {
            sendWithQueue(
                buildJSONObject(
                    "type", type, "id", seq.toString(),
                    "data", buildJSONObject(*data)
                ).toString()
            )
        } catch (exc: JSONException) {
            Log.e("EuphoriaWebSocketClient", "Exception while serializing JSON", exc)
            return -1
        }
        return seq
    }

    fun doClose(fin: Boolean) {
        var makeEvent = false
        synchronized(this) {
            if (!closed) {
                makeEvent = true
                closed = true
            }
        }
        if (makeEvent) submitEvent(CloseEventImpl(fin))
        close()
    }

    private fun submitEvent(evt: ConnectionEvent) {
        if (evt is IdentityEventImpl) {
            val e: IdentityEventImpl = evt
            sessions[e.identity.sessionID] = e.identity
        } else if (evt is PresenceChangeEventImpl) {
            val e: PresenceChangeEventImpl = evt
            if (e.isPresent) {
                for (s: ServerSessionView in e.sessionsEx) sessions.remove(s.sessionID)
            } else {
                for (s: ServerSessionView in e.sessionsEx) sessions[s.serverID] = s
            }
        } else if (evt is NickChangeEventImpl) {
            val e: NickChangeEventImpl = evt
            sessions[e.session.serverID] = e.session
        }
        parent.submitEvent(evt)
    }

    companion object {
        @Throws(JSONException::class)
        private fun buildJSONObject(vararg data: Any?): JSONObject {
            if (data.size % 2 != 0) throw IllegalArgumentException("Invalid JSON object construction shortcut")
            val ret = JSONObject()
            var i = 0
            while (i < data.size) {
                if (data[i] !is String) throw JSONException("Object key is not a string")
                if (data[i + 1] == null) {
                    i += 2
                    continue
                }
                ret.put(data[i] as String, data[i + 1])
                i += 2
            }
            return ret
        }

        @Throws(JSONException::class)
        private fun parseSessionView(source: JSONObject): ServerSessionView {
            return SessionViewImpl(
                source.getString("session_id"),
                source.getString("id"),
                source.getString("name"),
                source.optBoolean("is_staff"),
                source.optBoolean("is_manager"),
                source.getString("server_id"),
                source.getString("server_era")
            )
        }

        @Throws(JSONException::class)
        private fun parseMessage(source: JSONObject): Message {
            return MessageImpl(
                source.getString("id"),
                source.opt("parent") as String?,
                source.getLong("time"),
                parseSessionView(source.getJSONObject("sender")),
                source.getString("content"),
                source.optBoolean("truncated")
            )
        }

        @Throws(JSONException::class)
        private fun parseSessionViewArray(source: JSONArray): List<ServerSessionView> {
            val accum: MutableList<ServerSessionView> =
                ArrayList()
            for (i in 0 until source.length()) {
                accum.add(parseSessionView(source.getJSONObject(i)))
            }
            return Collections.unmodifiableList(accum)
        }

        @Throws(JSONException::class)
        private fun parseMessageArray(source: JSONArray): List<Message> {
            val accum: MutableList<Message> =
                ArrayList()
            for (i in 0 until source.length()) {
                accum.add(parseMessage(source.getJSONObject(i)))
            }
            return Collections.unmodifiableList(accum)
        }
    }

    init {
        sessions = HashMap()
        sendaheadQueue = ArrayDeque()
        ready = false
        closed = false
    }
}
