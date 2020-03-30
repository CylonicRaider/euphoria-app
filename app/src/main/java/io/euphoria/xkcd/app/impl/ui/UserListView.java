package io.euphoria.xkcd.app.impl.ui;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

/** Created by Xyzzy on 2020-03-30. */

public class UserListView extends RecyclerView {

    public UserListView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setLayoutManager(new LinearLayoutManager(context));
    }

}
