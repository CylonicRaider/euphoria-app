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

        // TODO debug this hard

        private final MessageTree base;
        private int startPos;
        private int endPos;
        private int displayTop;
        private int displayBottom;

        public IndentLine(MessageTree base) {
            this.base = base;
        }

        public MessageTree getBase() {
            return base;
        }

        public int getStartPos() {
            return startPos;
        }

        public void setStartPos(int startPos) {
            this.startPos = startPos;
        }

        public int getEndPos() {
            return endPos;
        }

        public void setEndPos(int endPos) {
            this.endPos = endPos;
        }

        public void draw(Canvas c, int top, int bottom) {
            if (startPos == endPos) return;
            top = Math.max(top, displayTop);
            bottom = Math.min(bottom, displayBottom);
            int x = indentBase + base.getIndent() * indentUnit;
            c.drawLine(x, top, x, bottom, indentPaint);
        }

        public boolean updatePositions(int topVisible, int bottomVisible) {
            int idx = ((MessageListAdapter) getAdapter()).indexOf(base);
            startPos = idx + 1;
            endPos = startPos + base.countVisibleReplies();
            if (startPos < topVisible && endPos < topVisible ||
                    startPos > bottomVisible + 1 && endPos > bottomVisible + 1)
                return false;
            if (startPos != endPos) {
                if (startPos < topVisible) {
                    displayTop = Integer.MIN_VALUE;
                } else {
                    ViewHolder h = findViewHolderForAdapterPosition(startPos);
                    displayTop = (h == null) ? Integer.MIN_VALUE : h.itemView.getTop();
                }
                if (endPos > bottomVisible) {
                    displayBottom = Integer.MAX_VALUE;
                } else {
                    ViewHolder h = findViewHolderForAdapterPosition(endPos);
                    displayBottom = (h == null) ? Integer.MAX_VALUE : h.itemView.getTop();
                }
            } else {
                /* NOP -- draw() will do nothing in this case. */
            }
            return true;
        }

        public void onScrolled(int dx, int dy) {
            if (displayTop != Integer.MIN_VALUE) displayTop -= dy;
            if (displayBottom != Integer.MAX_VALUE) displayBottom -= dy;
        }

    }

    private static final String TAG = "MessageListView";

    public static final int INDENT_LINE_OFFSET = 3;
    public static final int INDENT_LINE_WIDTH = 2;

    private final AdapterDataObserver adapterObserver = new AdapterDataObserver() {
        @Override
        public void onChanged() {
            updateLines();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            onChanged();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            onChanged();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            onChanged();
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            onChanged();
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
    private final List<IndentLine> lines;
    private final Map<MessageTree, IndentLine> linesBelow;

    public MessageListView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        /* Indent line painting */
        indentPaint = new Paint();
        indentPaint.setStrokeWidth(UIUtils.dpToPx(context, INDENT_LINE_WIDTH));
        indentPaint.setColor(ContextCompat.getColor(context, R.color.indent_line));
        indentBase = getPaddingLeft() + UIUtils.dpToPx(context, INDENT_LINE_OFFSET);
        indentUnit = MessageView.computeIndentWidth(context, 1);
        /* Indent line containers */
        lines = new LinkedList<>();
        linesBelow = new HashMap<>();
        /* Parent class configuration */
        LinearLayoutManager lm = new LinearLayoutManager(context);
        lm.setStackFromEnd(true);
        setLayoutManager(lm);
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
        BaseMessageView msgView = (BaseMessageView) child;
        MessageTree tree = msgView.getMessage();
        MessageListAdapter adapter = (MessageListAdapter) getAdapter();
        while (true) {
            if (tree.getID() != null) {
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
    }

    @Override
    public void onDraw(Canvas c) {
        super.onDraw(c);
        if (!lines.isEmpty()) {
            int top = 0, bottom = getHeight();
            for (IndentLine l : lines) l.draw(c, top, bottom);
        }
    }

    private void updateLines() {
        if (lineUpdaterScheduled) return;
        lineUpdaterScheduled = true;
        post(lineUpdater);
    }

}
