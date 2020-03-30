package io.euphoria.xkcd.app.impl.ui;

/** Created by Xyzzy on 2020-03-29. */

public class DisplayListenerAdapter implements DisplayListener {

    public static void notifyItemMovedLenient(DisplayListener listener, int from, int to) {
        if (from == -1 && to == -1) {
            /* NOP */
        } else if (from == -1) {
            listener.notifyItemRangeInserted(to, 1);
        } else if (to == -1) {
            listener.notifyItemRangeRemoved(from, 1);
        } else {
            listener.notifyItemMoved(from, to);
        }
    }

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
