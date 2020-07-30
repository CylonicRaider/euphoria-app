package io.euphoria.xkcd.app

import android.os.Bundle
import androidx.fragment.app.Fragment
import io.euphoria.xkcd.app.connection.ConnectionManager
import io.euphoria.xkcd.app.control.RoomController
import io.euphoria.xkcd.app.impl.connection.ConnectionManagerImpl
import io.euphoria.xkcd.app.impl.ui.RoomUIManagerImpl
import io.euphoria.xkcd.app.ui.RoomUIManager

/**
 * @author N00bySumairu
 */
class RoomControllerFragment : Fragment() {
    private lateinit var roomUIManager: RoomUIManager
    private lateinit var connManager: ConnectionManager
    lateinit var roomController: RoomController
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        roomUIManager = RoomUIManagerImpl()
        connManager = ConnectionManagerImpl()
        roomController =
            RoomController(activity!!.applicationContext, roomUIManager, connManager)
        retainInstance = true
    }

    override fun onDestroy() {
        super.onDestroy()
        roomController.shutdown()
    }

}
