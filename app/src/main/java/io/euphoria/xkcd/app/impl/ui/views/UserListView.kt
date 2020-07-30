package io.euphoria.xkcd.app.impl.ui.views

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/** Created by Xyzzy on 2020-03-30.  */
class UserListView(
    context: Context,
    attrs: AttributeSet?
) : RecyclerView(context, attrs) {
    init {
        layoutManager = LinearLayoutManager(context)
    }
}
