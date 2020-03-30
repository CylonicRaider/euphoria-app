package io.euphoria.xkcd.app.impl.ui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import io.euphoria.xkcd.app.R;
import io.euphoria.xkcd.app.connection.event.NickChangeEvent;
import io.euphoria.xkcd.app.connection.event.PresenceChangeEvent;

/** Created by Xyzzy on 2020-03-30. */

public class UserListAdapter extends RecyclerView.Adapter<UserListAdapter.ViewHolder> implements DisplayListener {

    class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(NicknameView itemView) {
            super(itemView);
        }

        public NicknameView getNicknameView() {
            return (NicknameView) itemView;
        }

    }

    private final UserList data;

    public UserListAdapter(UserList data) {
        this.data = data;
        data.setDisplayListener(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        NicknameView v = (NicknameView) inflater.inflate(R.layout.user_list_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.getNicknameView().setText(getItem(position).getNickname());
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public UserList.UIUser getItem(int index) {
        return data.get(index);
    }

    public UserList getData() {
        return data;
    }

    public void handle(PresenceChangeEvent evt) {
        if (evt.isPresent()) {
            data.addAll(evt.getSessions());
        } else {
            data.removeAll(evt.getSessions());
        }
    }

    public void handle(NickChangeEvent evt) {
        data.setNick(data.get(evt.getSession().getSessionID()), evt.getNewNick());
    }

}
