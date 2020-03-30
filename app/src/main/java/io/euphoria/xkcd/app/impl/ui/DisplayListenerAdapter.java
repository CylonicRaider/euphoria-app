package io.euphoria.xkcd.app.impl.ui;

/** Created by Xyzzy on 2020-03-29. */

public class DisplayListenerAdapter implements DisplayListener {

    public static DisplayListener NULL = new DisplayListenerAdapter();

    @Override
    public void notifyItemRangeInserted(int start, int length) {}

    @Override
    public void notifyItemChanged(int index) {}

    @Override
    public void notifyItemMoved(int from, int to) {}

    @Override
    public void notifyItemRangeRemoved(int start, int length) {}

}
