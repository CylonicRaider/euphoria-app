package io.euphoria.xkcd.app.impl.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.euphoria.xkcd.app.R
import io.euphoria.xkcd.app.impl.ui.UIUtils
import io.euphoria.xkcd.app.impl.ui.data.MessageTree
import java.util.*

/** Created by Xyzzy on 2017-10-02.  */
class MessageListView(
    context: Context,
    attrs: AttributeSet?
) : RecyclerView(context, attrs) {
    inner class IndentLine(val base: MessageTree) {
        private var startPos = 0
        private var endPos = 0
        private var displayTop = 0
        private var displayBottom = 0
        override fun toString(): String {
            return String.format(
                (null as Locale?)!!, "%s@%h[msg=%s,start=%d,end=%d,top=%d,bottom=%d]",
                javaClass.simpleName, this, base, startPos, endPos, displayTop, displayBottom
            )
        }

        fun draw(c: Canvas, top: Int, bottom: Int) {
            if (displayTop >= displayBottom) return
            val x = indentBase + base.indent * indentUnit
            c.drawLine(
                x.toFloat(),
                top.coerceAtLeast(displayTop).toFloat(),
                x.toFloat(),
                bottom.coerceAtMost(displayBottom).toFloat(),
                indentPaint
            )
        }

        fun updatePositions(topVisible: Int, bottomVisible: Int): Boolean {
            val idx = (adapter as MessageListAdapter).indexOf(base)
            startPos = idx + 1
            endPos = startPos + base.countVisibleReplies() - 1
            /* If we are definitely outside the visible area, discard us. */if (endPos < topVisible - 1 || startPos > bottomVisible + 1) return false
            /* Special case for not being visible at all. */if (endPos < startPos) {
                displayTop = 0
                displayBottom = 0
                return true
            }
            /* Otherwise, calculate our top and bottom. */displayTop = if (startPos < topVisible) {
                Int.MIN_VALUE
            } else {
                val h: ViewHolder? = findViewHolderForAdapterPosition(startPos)
                if (h == null) Int.MAX_VALUE else h.itemView.top + indentTopMargin
            }
            displayBottom = if (endPos > bottomVisible) {
                Int.MAX_VALUE
            } else {
                val h: ViewHolder? = findViewHolderForAdapterPosition(endPos)
                if (h == null) Int.MIN_VALUE else h.itemView.bottom - indentBottomMargin
            }
            return true
        }

        fun onScrolled(dx: Int, dy: Int) {
            if (displayTop != Int.MIN_VALUE && displayTop != Int.MAX_VALUE) displayTop -= dy
            if (displayBottom != Int.MIN_VALUE && displayBottom != Int.MAX_VALUE) displayBottom -= dy
        }

    }

    private inner class LayoutManager(context: Context?) :
        LinearLayoutManager(context) {
        override fun onLayoutCompleted(st: State?) {
            super.onLayoutCompleted(st)
            // Deferring the update causes a visible delay here; since we have a valid layout, we can as well do it
            // immediately.
            lineUpdater.run()
        }

        init {
            stackFromEnd = true
        }
    }

    private val adapterObserver: AdapterDataObserver = object : AdapterDataObserver() {
        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            var p = positionStart
            val e = positionStart + itemCount
            while (p < e) {
                val vh = findViewHolderForAdapterPosition(p)
                if (vh == null || vh.itemView !is MessageView) {
                    p++
                    continue
                }
                val mt = (vh.itemView as MessageView).message
                if (mt == null) {
                    p++
                    continue
                }
                val il = linesBelow.remove(mt)
                if (il != null) lines.remove(il)
                p++
            }
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            var p = fromPosition
            val e = fromPosition + itemCount
            while (p < e) {
                val vh = findViewHolderForAdapterPosition(p)
                if (vh == null || vh.itemView !is BaseMessageView) {
                    p++
                    continue
                }
                val mt = (vh.itemView as BaseMessageView).message
                if (mt == null) {
                    p++
                    continue
                }
                addIndentLinesFor(mt)
                p++
            }
        }
    }
    private val lineUpdater = Runnable {
        lineUpdaterScheduled = false
        val layout: LinearLayoutManager = layoutManager as LinearLayoutManager
        val topVisible: Int = layout.findFirstVisibleItemPosition()
        val bottomVisible: Int = layout.findLastVisibleItemPosition()
        val iter = lines.iterator()
        while (iter.hasNext()) {
            val il = iter.next()
            if (il.updatePositions(topVisible, bottomVisible)) continue
            iter.remove()
            linesBelow.remove(il.base)
        }
        invalidate()
    }
    private var lineUpdaterScheduled = false
    private val indentPaint: Paint
    private val indentBase: Int
    private val indentUnit: Int
    private val indentTopMargin: Int
    private val indentBottomMargin: Int
    private val lines: MutableList<IndentLine>
    private val linesBelow: MutableMap<MessageTree?, IndentLine>
    private var lastTopVisible: Int
    private var lastBottomVisible: Int
    override fun setAdapter(adapter: Adapter<*>?) {
        super.setAdapter(adapter)
        adapter?.registerAdapterDataObserver(adapterObserver)
    }

    override fun swapAdapter(adapter: Adapter<*>?, removeAndRecycleExistingViews: Boolean) {
        super.swapAdapter(adapter, removeAndRecycleExistingViews)
        adapter?.registerAdapterDataObserver(adapterObserver)
    }

    override fun onChildAttachedToWindow(child: View) {
        super.onChildAttachedToWindow(child)
        if (child !is BaseMessageView) return
        child.message?.also { addIndentLinesFor(it) }
        updateLines()
    }

    override fun onChildDetachedFromWindow(child: View) {
        super.onChildDetachedFromWindow(child)
        if (child !is BaseMessageView) return
        updateLines()
    }

    override fun onScrolled(dx: Int, dy: Int) {
        super.onScrolled(dx, dy)
        for (l in lines) l.onScrolled(dx, dy)
        /* Check if any new lines have to be added. */
        val layout =
            layoutManager as LayoutManager
        val topVisible: Int = layout.findFirstVisibleItemPosition()
        val bottomVisible: Int = layout.findLastVisibleItemPosition()
        if (topVisible != lastTopVisible || bottomVisible != lastBottomVisible) {
            if (lastTopVisible == -1 || lastBottomVisible == -1) {
                addIndentLinesFor(topVisible, bottomVisible)
            } else {
                addIndentLinesFor(topVisible, lastTopVisible)
                addIndentLinesFor(lastBottomVisible, bottomVisible)
            }
            lastTopVisible = topVisible
            lastBottomVisible = bottomVisible
            updateLines()
        }
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        if (lines.isNotEmpty()) {
            val top = 0
            val bottom: Int = height
            for (l in lines) l.draw(c, top, bottom)
        }
    }

    private val messageListAdapter: MessageListAdapter?
        get() = adapter as? MessageListAdapter

    private fun addIndentLinesFor(tree: MessageTree) {
        val adapter = messageListAdapter ?: return
        var tree = tree
        while (true) {
            if (tree.id != MessageTree.CURSOR_ID) {
                var il = linesBelow[tree]
                if (il == null) {
                    il = IndentLine(tree)
                    lines.add(il)
                    linesBelow[tree] = il
                }
            }
            if (tree.parent == null) break
            tree = adapter[tree.parent!!]!!
        }
    }

    private fun addIndentLinesFor(minIdx: Int, maxIdx: Int) {
        val adapter = messageListAdapter ?: return
        (minIdx..maxIdx).mapNotNull { adapter.getItem(it) }.forEach { addIndentLinesFor(it) }
    }

    private fun updateLines() {
        if (lineUpdaterScheduled) return
        lineUpdaterScheduled = true
        post(lineUpdater)
    }

    companion object {
        private const val TAG = "MessageListView"
        const val INDENT_LINE_OFFSET = 9
        const val INDENT_LINE_WIDTH = 2
        const val INDENT_LINE_TOP_MARGIN = 1
        const val INDENT_LINE_BOTTOM_MARGIN = 1
    }

    init {
        /* Indent line painting */indentPaint = Paint()
        indentPaint.strokeWidth = UIUtils.dpToPx(
            context,
            INDENT_LINE_WIDTH
        ).toFloat()
        indentPaint.color = ContextCompat.getColor(context, R.color.indent_line)
        indentBase =
            paddingLeft + UIUtils.dpToPx(context, INDENT_LINE_OFFSET)
        indentUnit = BaseMessageView.computeIndentWidth(context, 1)
        indentTopMargin = UIUtils.dpToPx(context, INDENT_LINE_TOP_MARGIN)
        indentBottomMargin =
            UIUtils.dpToPx(context, INDENT_LINE_BOTTOM_MARGIN)
        /* Indent line containers */lines = LinkedList()
        linesBelow = HashMap()
        /* Scrolling detection */lastTopVisible = -1
        lastBottomVisible = -1
        /* Parent class configuration */layoutManager = LayoutManager(context)
        // FIXME: Re-add animations.
        //if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
        itemAnimator = null
    }
}
