package io.euphoria.xkcd.app.impl.ui.data;

/** Created by Xyzzy on 2020-03-29. */

public interface DisplayListener {

    void notifyItemRangeInserted(int start, int length);

    void notifyItemChanged(int index);

    void notifyItemMoved(int from, int to);

    void notifyItemRangeRemoved(int start, int length);

}
