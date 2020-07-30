package io.euphoria.xkcd.app.impl.ui.data

/** Created by Xyzzy on 2020-03-29.  */
class DisplayListenerAdapter : DisplayListener {
    override fun notifyItemRangeInserted(start: Int, length: Int) {}
    override fun notifyItemChanged(index: Int) {}
    override fun notifyItemMoved(from: Int, to: Int) {}
    override fun notifyItemRangeRemoved(start: Int, length: Int) {}

    companion object {
        fun notifyItemMovedLenient(
            listener: DisplayListener?,
            from: Int,
            to: Int
        ) {
            if (from == -1 && to == -1) {
                /* NOP */
            } else if (from == -1) {
                listener!!.notifyItemRangeInserted(to, 1)
            } else if (to == -1) {
                listener!!.notifyItemRangeRemoved(from, 1)
            } else {
                listener!!.notifyItemMoved(from, to)
            }
        }

        val NULL: DisplayListener = DisplayListenerAdapter()
    }
}
