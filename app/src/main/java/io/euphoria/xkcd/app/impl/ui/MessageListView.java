package io.euphoria.xkcd.app.impl.ui;

import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

/** Created by Xyzzy on 2017-10-02. */

public class MessageListView extends RecyclerView {

    private static final String TAG = "MessageListView";

    public MessageListView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        LinearLayoutManager lm = new LinearLayoutManager(context);
        lm.setStackFromEnd(true);
        setLayoutManager(lm);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            setItemAnimator(null);
        }
    }

}
