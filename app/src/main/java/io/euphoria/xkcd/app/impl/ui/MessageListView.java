package io.euphoria.xkcd.app.impl.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.euphoria.xkcd.app.R;

/** Created by Xyzzy on 2017-10-02. */

public class MessageListView extends RecyclerView {

    public class IndentLine {

        private final MessageTree base;
        private int startPos;
        private int endPos;
        private int displayTop;
        private int displayBottom;

        public IndentLine(MessageTree base) {
            this.base = base;
        }

        public String toString() {
            return String.format((Locale) null, "%s@%h[msg=%s,start=%d,end=%d,top=%d,bottom=%d]",
                    getClass().getSimpleName(), this, base, startPos, endPos, displayTop, displayBottom);
        }

        public MessageTree getBase() {
            return base;
        }

        public void draw(Canvas c, int top, int bottom) {
            if (displayTop >= displayBottom) return;
            top = Math.max(top, displayTop);
            bottom = Math.min(bottom, displayBottom);
            int x = indentBase + base.getIndent() * indentUnit;
            c.drawLine(x, top, x, bottom, indentPaint);
        }

        public boolean updatePositions(int topVisible, int bottomVisible) {
            int idx = ((MessageListAdapter) getAdapter()).indexOf(base);
            startPos = idx + 1;
            endPos = startPos + base.countVisibleReplies() - 1;
            /* If we are definitely outside the visible area, discard us. */
            if (endPos < topVisible - 1 || startPos > bottomVisible + 1)
                return false;
            /* Special case for not being visible at all. */
            if (endPos < startPos) {
                displayTop = 0;
                displayBottom = 0;
                return true;
            }
            /* Otherwise, calculate our top and bottom. */
            if (startPos < topVisible) {
                displayTop = Integer.MIN_VALUE;
            } else {
                ViewHolder h = findViewHolderForAdapterPosition(startPos);
                displayTop = (h == null) ? Integer.MAX_VALUE : h.itemView.getTop() + indentTopMargin;
            }
            if (endPos > bottomVisible) {
                displayBottom = Integer.MAX_VALUE;
            } else {
                ViewHolder h = findViewHolderForAdapterPosition(endPos);
                displayBottom = (h == null) ? Integer.MIN_VALUE : h.itemView.getBottom() - indentBottomMargin;
            }
            return true;
        }

        public void onScrolled(int dx, int dy) {
            if (displayTop != Integer.MIN_VALUE && displayTop != Integer.MAX_VALUE) displayTop -= dy;
            if (displayBottom != Integer.MIN_VALUE && displayBottom != Integer.MAX_VALUE) displayBottom -= dy;
        }

    }

    private class LayoutManager extends LinearLayoutManager {

        public LayoutManager(Context context) {
            super(context);
            setStackFromEnd(true);
        }

        @Override
        public void onLayoutCompleted(State st) {
            super.onLayoutCompleted(st);
            // Deferring the update causes a visible delay here; since we have a valid layout, we can as well do it
            // immediately.
            lineUpdater.run();
        }

    }

    private static final String TAG = "MessageListView";

    public static final int INDENT_LINE_OFFSET = 9;
    public static final int INDENT_LINE_WIDTH = 2;
    public static final int INDENT_LINE_TOP_MARGIN = 1;
    public static final int INDENT_LINE_BOTTOM_MARGIN = 1;

    private final AdapterDataObserver adapterObserver = new AdapterDataObserver() {

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            for (int p = positionStart, e = positionStart + itemCount; p < e; p++) {
                ViewHolder vh = findViewHolderForAdapterPosition(p);
                if (vh == null || !(vh.itemView instanceof MessageView)) continue;
                MessageTree mt = ((MessageView) vh.itemView).getMessage();
                if (mt == null) continue;
                IndentLine il = linesBelow.remove(mt);
                if (il != null) lines.remove(il);
            }
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            for (int p = fromPosition, e = fromPosition + itemCount; p < e; p++) {
                ViewHolder vh = findViewHolderForAdapterPosition(p);
                if (vh == null || !(vh.itemView instanceof BaseMessageView)) continue;
                MessageTree mt = ((BaseMessageView) vh.itemView).getMessage();
                if (mt == null) continue;
                addIndentLinesFor(mt);
            }
        }

    };

    private final Runnable lineUpdater = new Runnable() {
        @Override
        public void run() {
            lineUpdaterScheduled = false;
            LinearLayoutManager layout = (LinearLayoutManager) getLayoutManager();
            int topVisible = layout.findFirstVisibleItemPosition();
            int bottomVisible = layout.findLastVisibleItemPosition();
            Iterator<IndentLine> iter = lines.iterator();
            while (iter.hasNext()) {
                IndentLine il = iter.next();
                if (il.updatePositions(topVisible, bottomVisible)) continue;
                iter.remove();
                linesBelow.remove(il.getBase());
            }
            invalidate();
        }
    };
    private boolean lineUpdaterScheduled = false;

    private final Paint indentPaint;
    private final int indentBase;
    private final int indentUnit;
    private final int indentTopMargin;
    private final int indentBottomMargin;
    private final List<IndentLine> lines;
    private final Map<MessageTree, IndentLine> linesBelow;
    private int lastTopVisible;
    private int lastBottomVisible;

    public MessageListView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        /* Indent line painting */
        indentPaint = new Paint();
        indentPaint.setStrokeWidth(UIUtils.dpToPx(context, INDENT_LINE_WIDTH));
        indentPaint.setColor(ContextCompat.getColor(context, R.color.indent_line));
        indentBase = getPaddingLeft() + UIUtils.dpToPx(context, INDENT_LINE_OFFSET);
        indentUnit = MessageView.computeIndentWidth(context, 1);
        indentTopMargin = UIUtils.dpToPx(context, INDENT_LINE_TOP_MARGIN);
        indentBottomMargin = UIUtils.dpToPx(context, INDENT_LINE_BOTTOM_MARGIN);
        /* Indent line containers */
        lines = new LinkedList<>();
        linesBelow = new HashMap<>();
        /* Scrolling detection */
        lastTopVisible = -1;
        lastBottomVisible = -1;
        /* Parent class configuration */
        setLayoutManager(new LayoutManager(context));
        // FIXME: Re-add animations.
        //if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
        setItemAnimator(null);
    }

    @Override
    public void setAdapter(Adapter adapter) {
        super.setAdapter(adapter);
        adapter.registerAdapterDataObserver(adapterObserver);
    }

    @Override
    public void swapAdapter(Adapter adapter, boolean removeAndRecycleExistingViews) {
        super.swapAdapter(adapter, removeAndRecycleExistingViews);
        adapter.registerAdapterDataObserver(adapterObserver);
    }

    @Override
    public void onChildAttachedToWindow(View child) {
        super.onChildAttachedToWindow(child);
        if (!(child instanceof BaseMessageView)) return;
        addIndentLinesFor(((BaseMessageView) child).getMessage());
        updateLines();
    }

    @Override
    public void onChildDetachedFromWindow(View child) {
        super.onChildDetachedFromWindow(child);
        if (!(child instanceof BaseMessageView)) return;
        updateLines();
    }

    @Override
    public void onScrolled(int dx, int dy) {
        super.onScrolled(dx, dy);
        for (IndentLine l : lines) l.onScrolled(dx, dy);
        /* Check if any new lines have to be added. */
        LayoutManager layout = (LayoutManager) getLayoutManager();
        int topVisible = layout.findFirstVisibleItemPosition();
        int bottomVisible = layout.findLastVisibleItemPosition();
        if (topVisible != lastTopVisible || bottomVisible != lastBottomVisible) {
            if (lastTopVisible == -1 || lastBottomVisible == -1) {
                addIndentLinesFor(topVisible, bottomVisible);
            } else {
                addIndentLinesFor(topVisible, lastTopVisible);
                addIndentLinesFor(lastBottomVisible, bottomVisible);
            }
            lastTopVisible = topVisible;
            lastBottomVisible = bottomVisible;
            updateLines();
        }
    }

    @Override
    public void onDraw(Canvas c) {
        super.onDraw(c);
        if (!lines.isEmpty()) {
            int top = 0, bottom = getHeight();
            for (IndentLine l : lines) l.draw(c, top, bottom);
        }
    }

    private void addIndentLinesFor(MessageTree tree) {
        MessageListAdapter adapter = (MessageListAdapter) getAdapter();
        while (true) {
            if (!tree.getID().equals(MessageTree.CURSOR_ID)) {
                IndentLine il = linesBelow.get(tree);
                if (il == null) {
                    il = new IndentLine(tree);
                    lines.add(il);
                    linesBelow.put(tree, il);
                }
            }
            if (tree.getParent() == null) break;
            tree = adapter.get(tree.getParent());
        }
    }

    private void addIndentLinesFor(int minIdx, int maxIdx) {
        MessageListAdapter adapter = (MessageListAdapter) getAdapter();
        for (int i = minIdx; i <= maxIdx; i++) {
            addIndentLinesFor(adapter.getItem(i));
        }
    }

    private void updateLines() {
        if (lineUpdaterScheduled) return;
        lineUpdaterScheduled = true;
        post(lineUpdater);
    }

}
