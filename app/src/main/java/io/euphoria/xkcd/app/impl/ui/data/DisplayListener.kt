package io.euphoria.xkcd.app.impl.ui.data

/** Created by Xyzzy on 2020-03-29.  */
interface DisplayListener {
    fun notifyItemRangeInserted(start: Int, length: Int)
    fun notifyItemChanged(index: Int)
    fun notifyItemMoved(from: Int, to: Int)
    fun notifyItemRangeRemoved(start: Int, length: Int)
}
