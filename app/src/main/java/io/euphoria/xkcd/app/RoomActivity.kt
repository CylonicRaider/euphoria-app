package io.euphoria.xkcd.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Parcelable
import android.util.SparseArray
import android.view.*
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.euphoria.xkcd.app.connection.ConnectionStatus
import io.euphoria.xkcd.app.control.RoomController
import io.euphoria.xkcd.app.data.Message
import io.euphoria.xkcd.app.databinding.ActivityRoomBinding
import io.euphoria.xkcd.app.databinding.InputBarBinding
import io.euphoria.xkcd.app.impl.ui.RoomUIImpl
import io.euphoria.xkcd.app.impl.ui.data.MessageForest
import io.euphoria.xkcd.app.impl.ui.data.UserList
import io.euphoria.xkcd.app.impl.ui.views.InputBarView
import io.euphoria.xkcd.app.impl.ui.views.InputBarView.NickChangeListener
import io.euphoria.xkcd.app.impl.ui.views.InputBarView.SubmitListener
import io.euphoria.xkcd.app.impl.ui.views.MessageListAdapter
import io.euphoria.xkcd.app.impl.ui.views.MessageListAdapter.InputBarDirection
import io.euphoria.xkcd.app.impl.ui.views.MessageListAdapter.InputBarListener
import io.euphoria.xkcd.app.impl.ui.views.MessageListView
import io.euphoria.xkcd.app.impl.ui.views.UserListAdapter
import io.euphoria.xkcd.app.ui.RoomUI
import io.euphoria.xkcd.app.ui.RoomUIFactory
import io.euphoria.xkcd.app.ui.event.LogRequestEvent
import io.euphoria.xkcd.app.ui.event.MessageSendEvent
import io.euphoria.xkcd.app.ui.event.NewNickEvent

class RoomActivity : FragmentActivity() {
    private inner class LocalRoomUIFactory : RoomUIFactory {
        override fun createRoomUI(roomName: String): RoomUI {
            return LocalRoomUIImpl(roomName)
        }
    }

    private inner class LocalRoomUIImpl constructor(roomName: String) :
        RoomUIImpl(roomName) {
        var connectionStatus: ConnectionStatus? = null
            private set

        override fun setConnectionStatus(status: ConnectionStatus) {
            super.setConnectionStatus(status)
            if (connectionStatus != ConnectionStatus.CONNECTED && status == ConnectionStatus.CONNECTED) trimLogs()
            connectionStatus = status
        }

        override fun showMessages(messages: List<Message>) {
            super.showMessages(messages)
            // An empty response signifies no more logs.
            if (messages.isEmpty()) return

            var earliestID = messages[0].id
            for (msg in messages.drop(1)) {
                if (msg.id < earliestID) {
                    earliestID = msg.id
                }
            }
            this@RoomActivity.earliestID = earliestID

            isPullingLogs = false
            checkPullLogs()
        }
    }

    private val TAG: String = "RoomActivity"
    private lateinit var roomControllerFragment: RoomControllerFragment
    private lateinit var roomController: RoomController
    private lateinit var roomUI: LocalRoomUIImpl

    private lateinit var binding: ActivityRoomBinding
    private lateinit var inputBarBinding: InputBarBinding

    private lateinit var messageAdapter: MessageListAdapter
    private lateinit var userListAdapter: UserListAdapter

    private var isPullingLogs: Boolean = false
    private var earliestID: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Quickly bounce away if we have a wrong room
        val i: Intent = intent
        if (Intent.ACTION_VIEW != i.action || !URLs.isValidRoomUri(i.data)) {
            val chooserIntent: Intent = Intent(this, MainActivity::class.java)
            startActivity(chooserIntent)
            return
        }

        // Create the UI
        binding = ActivityRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val roomName: String = URLs.getRoomName(i.data!!)!! // i.data being a valid room URI was checked above
        title = "&$roomName"

        // Get RoomControllerFragment
        val fm: FragmentManager = supportFragmentManager
        roomControllerFragment =
            fm.findFragmentByTag(TAG_ROOM_CONTROLLER_FRAGMENT)?.let {
                it as RoomControllerFragment
            } ?: RoomControllerFragment().also {
                // add the fragment
                fm.beginTransaction()
                    .add(it, TAG_ROOM_CONTROLLER_FRAGMENT)
                    .commit()
                fm.executePendingTransactions()
            }

        // Acquire RoomController and roomUI
        roomController = roomControllerFragment.roomController
        roomController.roomUIManager.setRoomUIFactory(LocalRoomUIFactory())
        roomUI = roomController.roomUIManager.getRoomUI(roomName) as LocalRoomUIImpl // FIXME: ugly and most likely unnecessary unchecked cast

        // View setup
        inputBarBinding = InputBarBinding.inflate(layoutInflater, binding.messageList, false)

        // Data setup
        var messages: MessageForest? = null
        var users: UserList? = null
        if (savedInstanceState != null) {
            messages = savedInstanceState.getParcelable(KEY_MESSAGES)
            users = savedInstanceState.getParcelable(KEY_USERS)
            val inputState: SparseArray<Parcelable>? =
                savedInstanceState.getSparseParcelableArray(KEY_INPUT_STATE)
            if (inputState != null) inputBarBinding.inputBar.restoreHierarchyState(inputState)
        }
        if (messages == null) {
            messages = MessageForest()
        }
        if (users == null) {
            users = UserList()
        }
        messageAdapter = MessageListAdapter(messages, inputBarBinding.inputBar)
        userListAdapter = UserListAdapter(users)
        binding.messageList.adapter = messageAdapter
        binding.userList.layoutManager = LinearLayoutManager(binding.userList.context)
        binding.userList.adapter = userListAdapter

        // Input bar setup
        messageAdapter.inputBarListener = (object : InputBarListener {
            override fun onInputBarMoved(
                oldParent: String?,
                newParent: String?
            ) {
                Handler(mainLooper).post { inputBarBinding.inputBar.requestEntryFocus() }
            }
        })
        inputBarBinding.inputBar.requestEntryFocus()
        inputBarBinding.messageEntry.setOnKeyListener(object : View.OnKeyListener {
            override fun onKey(
                v: View,
                keyCode: Int,
                event: KeyEvent
            ): Boolean {
                if (event.action != KeyEvent.ACTION_DOWN) return false
                val dir: InputBarDirection = when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP -> InputBarDirection.UP
                    KeyEvent.KEYCODE_DPAD_DOWN -> InputBarDirection.DOWN
                    KeyEvent.KEYCODE_DPAD_LEFT -> InputBarDirection.LEFT
                    KeyEvent.KEYCODE_DPAD_RIGHT ->                         // Upstream Euphoria binds the right key to the ROOT action, and we mirror that as default.
                        // TODO ask community whether they like it
                        if (RIGHT_KEY_HACK) InputBarDirection.ROOT else InputBarDirection.RIGHT
                    KeyEvent.KEYCODE_ESCAPE -> InputBarDirection.ROOT
                    else -> return false
                }
                return inputBarBinding.inputBar.mayNavigateInput(dir) && messageAdapter.navigateInputBar(dir)
            }
        })

        // Scrolling setup
        binding.messageList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy == 0) return
                checkPullLogs()
            }
        })

        // Controller etc. setup
        roomUI.link(binding.connStatusDisplay, messageAdapter, userListAdapter)
        roomController.openRoom(roomName)
        inputBarBinding.inputBar.nickChangeListener = (object : NickChangeListener {
            override fun onChangeNick(view: InputBarView): Boolean {
                val newNick: String? = view.nickText
                if (newNick!!.isEmpty() || roomUI.connectionStatus != ConnectionStatus.CONNECTED) return false
                roomUI.submitEvent(object : NewNickEvent {
                    override val newNick: String? = newNick
                    override val roomUI: RoomUI? = this@RoomActivity.roomUI

                })
                return true
            }
        })
        inputBarBinding.inputBar.submitListener = (object : SubmitListener {
            override fun onSubmit(view: InputBarView): Boolean {
                val text: String? = view.messageText
                val parent: String? = view.message!!.parent
                if (text!!.isEmpty() || roomUI.connectionStatus != ConnectionStatus.CONNECTED) return false
                roomUI.submitEvent(object : MessageSendEvent {
                    override val text: String? = text

                    override val parent: String? = parent

                    override val roomUI: RoomUI? = this@RoomActivity.roomUI
                })
                return true
            }
        })
        // Suspend log pulling until the snapshot-event arrives.
        isPullingLogs = true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.actions_room, menu)
        return true
    }

    fun toggleUserDrawer(item: MenuItem?) {
        val dl: DrawerLayout = findViewById(R.id.room_drawer_root)
        if (dl.isDrawerOpen(GravityCompat.END)) {
            dl.closeDrawer(GravityCompat.END)
        } else {
            dl.openDrawer(GravityCompat.END)
        }
    }

    fun showAbout(item: MenuItem?) {
        startActivity(Intent(this, AboutActivity::class.java))
    }

    override fun onBackPressed() {
        val dl: DrawerLayout = findViewById(R.id.room_drawer_root)
        if (dl.isDrawerOpen(GravityCompat.END)) {
            dl.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressed()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(KEY_MESSAGES, messageAdapter.data)
        outState.putParcelable(KEY_USERS, userListAdapter.data)
        // RecyclerView suppresses instance state saving for all its children; we override this decision for the
        // input bar without poking into RecyclerView internals.
        val inputState: SparseArray<Parcelable> = SparseArray()
        inputBarBinding.inputBar.saveHierarchyState(inputState)
        outState.putSparseParcelableArray(KEY_INPUT_STATE, inputState)
    }

    override fun onDestroy() {
        super.onDestroy()
        roomUI.unlink(binding.connStatusDisplay, messageAdapter, userListAdapter)
        roomController.closeRoom(roomUI.roomName)
        roomController.roomUIManager.setRoomUIFactory(null)
        roomController.shutdown()
    }

    private fun checkPullLogs() {
        val layout: LinearLayoutManager? = binding.messageList.layoutManager as LinearLayoutManager // TODO: do away with ugly cast if possible
        val adapter: RecyclerView.Adapter<*>? = binding.messageList.adapter
        if (layout == null || adapter == null) return
        if (layout.findFirstVisibleItemPosition() > adapter.itemCount * LOG_PULL_THRESHOLD) return
        pullMoreLogs()
    }

    private fun pullMoreLogs() {
        if (isPullingLogs) return
        isPullingLogs = true
        val before: String? = earliestID
        roomUI.submitEvent(object : LogRequestEvent {
            override val before: String? = before

            override val roomUI: RoomUI? = this@RoomActivity.roomUI
        })
    }

    private fun trimLogs() {
        val adapter: MessageListAdapter = binding.messageList.adapter as MessageListAdapter
        adapter.data.clear()
    }

    companion object {
        // TODO find some appropriate place for this in config
        val RIGHT_KEY_HACK: Boolean = true
        private val LOG_PULL_THRESHOLD: Double = 0.1
        private val KEY_MESSAGES: String = "messages"
        private val KEY_USERS: String = "users"
        private val KEY_INPUT_STATE: String = "inputState"

        // Tag for finding RoomControllerFragment
        private val TAG_ROOM_CONTROLLER_FRAGMENT: String =
            RoomControllerFragment::class.java.simpleName
    }
}
