package io.euphoria.xkcd.app.impl.ui.views

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.euphoria.xkcd.app.R
import io.euphoria.xkcd.app.impl.ui.data.DisplayListener
import io.euphoria.xkcd.app.impl.ui.data.UserList
import io.euphoria.xkcd.app.impl.ui.data.UserList.UIUser

/** Created by Xyzzy on 2020-03-30.  */
class UserListAdapter(val data: UserList) :
    RecyclerView.Adapter<UserListAdapter.ViewHolder?>(),
    DisplayListener {
    class ViewHolder(itemView: NicknameView) :
        RecyclerView.ViewHolder(itemView) {
        val nicknameView: NicknameView
            get() = itemView as NicknameView
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val inflater = parent.context.getSystemService(
            Context.LAYOUT_INFLATER_SERVICE
        ) as LayoutInflater
        val v =
            inflater.inflate(R.layout.user_list_item, parent, false) as NicknameView
        return ViewHolder(v)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        holder.nicknameView.text = getItem(position)!!.nickname
    }

    override fun getItemCount() = data.size()

    fun getItem(index: Int): UIUser? {
        return data[index]
    }

    init {
        data.setDisplayListener(this)
    }
}
