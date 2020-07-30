package io.euphoria.xkcd.app.impl.ui.views

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.euphoria.xkcd.app.R
import io.euphoria.xkcd.app.data.Message
import io.euphoria.xkcd.app.impl.ui.data.DisplayListener
import io.euphoria.xkcd.app.impl.ui.data.MessageForest
import io.euphoria.xkcd.app.impl.ui.data.MessageTree
import io.euphoria.xkcd.app.impl.ui.data.UIMessage

class MessageListAdapter(// The main data structure
    val data: MessageForest, // View of the input bar
    private val inputBar: InputBarView
) :
    RecyclerView.Adapter<MessageListAdapter.ViewHolder>(),
    DisplayListener {
    interface InputBarListener {
        fun onInputBarMoved(oldParent: String?, newParent: String?)
    }

    enum class InputBarDirection {
        UP, DOWN, LEFT, RIGHT, ROOT
    }

    // MessageTree representation of the input bar
    private var inputBarTree: MessageTree = data[MessageTree.Companion.CURSOR_ID] ?: MessageTree(null).also { data.add(it) }
    var inputBarListener: InputBarListener? = null
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val inflater = parent.context.getSystemService(
            Context.LAYOUT_INFLATER_SERVICE
        ) as LayoutInflater
        return when (viewType) {
            MESSAGE -> {
                val mc =
                    inflater.inflate(R.layout.template_message, parent, false) as MessageView
                mc.visibility = View.INVISIBLE
                ViewHolder(mc)
            }
            INPUT_BAR -> {
                inputBar.visibility = View.INVISIBLE
                ViewHolder(inputBar)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        when (holder.itemViewType) {
            MESSAGE -> {
                val mc = holder.itemView as MessageView
                mc.recycle()
                val mt = getItem(position)!!
                mc.setMessage(mt)
                mc.setTextClickListener(View.OnClickListener { moveInputBarAround(mt) })
                mc.setCollapserClickListener(View.OnClickListener { toggleCollapse(mt) })
                mc.visibility = View.VISIBLE
            }
            INPUT_BAR -> {
                val ib = holder.itemView as InputBarView
                ib.setIndent(getItem(position)!!.indent)
                ib.visibility = View.VISIBLE
            }
        }
    }

    override fun getItemCount() = data.size()

    fun getItem(position: Int): MessageTree? {
        return data[position]
    }

    override fun getItemId(position: Int): Long {
        return getItem(position)!!.longID
    }

    override fun getItemViewType(position: Int): Int {
        val mt = getItem(position)!!
        return if (mt.message == null) INPUT_BAR else MESSAGE
    }

    private fun dispatchInputBarMoved(
        oldParentID: String?,
        newParentID: String?
    ) {
        if (inputBarListener != null) inputBarListener!!.onInputBarMoved(oldParentID, newParentID)
    }

    fun indexOf(mt: MessageTree): Int {
        return data.indexOf(mt)
    }

    operator fun get(id: String): MessageTree? {
        return data[id]
    }

    fun getTree(message: Message): MessageTree? {
        return get(message.id)
    }

    fun getParent(tree: MessageTree): MessageTree? {
        return data.getParent(tree)
    }

    fun getReply(mt: MessageTree, index: Int): MessageTree? {
        return data.getReply(mt, index)
    }

    fun getSibling(mt: MessageTree, offset: Int): MessageTree? {
        return data.getSibling(mt, offset)
    }

    fun clear() {
        data.clear()
        data.add(inputBarTree)
    }

    fun add(mt: MessageTree): MessageTree? {
        return data.add(mt)
    }

    fun add(message: UIMessage): MessageTree? {
        return data.add(message)
    }

    fun remove(mt: MessageTree) {
        data.remove(mt, false)
    }

    // FIXME: Needs a better name.
    fun moveInputBarAround(mt: MessageTree?) {
        val preferredID: String?
        val alternateID: String?
        if (mt!!.parent == null) {
            preferredID = mt.id
            alternateID = null
        } else {
            preferredID = mt.parent
            alternateID = mt.id
        }
        moveInputBar(if (preferredID == inputBarTree.parent) alternateID else preferredID)
    }

    fun moveInputBar(newParentID: String?) {
        val oldParentID = inputBarTree.parent
        data.move(inputBarTree, newParentID?.let { get(it) }, ensureVisible = true)
        inputBar.setIndent(inputBarTree.indent)
        dispatchInputBarMoved(oldParentID, newParentID)
    }

    fun navigateInputBar(dir: InputBarDirection): Boolean {
        when (dir) {
            InputBarDirection.UP -> {
                var node: MessageTree? = inputBarTree
                do {
                    val pred = getSibling(node!!, -1)
                    if (pred != null) {
                        moveInputBar(pred.id)
                        return true
                    }
                    node = getParent(node)
                } while (node != null)
                return false
            }
            InputBarDirection.DOWN -> {
                if (inputBarTree.parent == null) return false
                val par = getParent(inputBarTree)!!
                var succ = getSibling(par, 1)
                if (succ != null) {
                    // <n00b> cannot use infinite for loop idiom because of auto-formatter ;-; </noob>
                    while (true) {
                        val child = getReply(succ!!, 0) ?: break
                        succ = child
                    }
                    moveInputBar(succ!!.id)
                } else {
                    moveInputBar(par.parent)
                }
                return true
            }
            InputBarDirection.LEFT -> {
                if (inputBarTree.parent == null) return false
                val parent = getParent(inputBarTree)
                moveInputBar(parent!!.parent)
                return true
            }
            InputBarDirection.RIGHT -> {
                val pred = getSibling(inputBarTree, -1) ?: return false
                moveInputBar(pred.id)
                return true
            }
            InputBarDirection.ROOT -> {
                moveInputBar(null)
                return true
            }
        }
        // when is exhaustive
    }

    fun tryEnsureVisible(mt: MessageTree, expand: Boolean): Boolean {
        return data.tryEnsureVisible(mt, expand)
    }

    fun toggleCollapse(mt: MessageTree, newState: Boolean) {
        data.setCollapsed(mt, newState)
    }

    fun toggleCollapse(mt: MessageTree) {
        data.toggleCollapsed(mt)
    }

    class ViewHolder(val itemMessageView: BaseMessageView) : RecyclerView.ViewHolder(
        itemMessageView
    )

    companion object {
        // For logging
        private const val TAG = "MessageListAdapter"

        // For view identification
        private const val MESSAGE = 0
        private const val INPUT_BAR = 1
    }

    init {
        data.setListener(this)
        inputBar.recycle()
        inputBar.setMessage(inputBarTree)
        setHasStableIds(true)
    }
}
