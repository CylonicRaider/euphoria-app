package io.euphoria.xkcd.app.impl.ui

import io.euphoria.xkcd.app.impl.ui.RoomUIManagerImpl
import io.euphoria.xkcd.app.ui.RoomUI
import io.euphoria.xkcd.app.ui.RoomUIFactory
import io.euphoria.xkcd.app.ui.RoomUIManager
import io.euphoria.xkcd.app.ui.UIManagerListener
import io.euphoria.xkcd.app.ui.event.RoomSwitchEvent
import java.util.*

/** Created by Xyzzy on 2017-02-24.  */ /* Implementation of RoomUIManager */
class RoomUIManagerImpl : RoomUIManager {
    private class DefaultRoomUIFactory : RoomUIFactory {
        override fun createRoomUI(roomName: String): RoomUI {
            return RoomUIImpl(roomName)
        }

        companion object {
            val INSTANCE = DefaultRoomUIFactory()
        }
    }

    private val listeners: MutableSet<UIManagerListener> =
        HashSet()
    private val roomUIs =
        HashMap<String, RoomUI>()
    private var factory: RoomUIFactory = DefaultRoomUIFactory.INSTANCE
    override fun setRoomUIFactory(factory: RoomUIFactory?) {
        this.factory =
            when {
                factory == null -> DefaultRoomUIFactory.INSTANCE
                factory !== this.factory -> factory.also { roomUIs.clear() }
                else -> factory
            }
    }

    override fun getRoomUI(roomName: String): RoomUI =
        roomUIs[roomName] ?: factory.createRoomUI(roomName).also { roomUIs[roomName] = it }

    /**
     * Adds an UIManagerListener to the RoomUIManagerImpl.
     * If the listener object is already registered,
     * the method will not register it again.
     *
     * @param l Listener to add
     */
    override fun addEventListener(l: UIManagerListener) {
        listeners.add(l)
    }

    /**
     * Removes an UIManagerListener from the RoomUIManagerImpl.
     * If the listener object is not registered,
     * the method will change nothing.
     *
     * @param l Listener to remove
     */
    override fun removeEventListener(l: UIManagerListener) {
        listeners.remove(l)
    }

    fun onRoomSwitch(roomName: String) {
        for (listener in listeners) {
            listener.onRoomSwitch(object : RoomSwitchEvent {
                override val roomName: String
                    get() = roomName

                override val roomUI: RoomUI?
                    get() = this@RoomUIManagerImpl.getRoomUI(roomName)
            })
        }
    }
}
