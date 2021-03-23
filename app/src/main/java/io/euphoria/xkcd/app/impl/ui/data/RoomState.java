package io.euphoria.xkcd.app.impl.ui.data;

import android.os.Parcelable;
import android.util.SparseArray;

import io.euphoria.xkcd.app.impl.ui.views.MessageListAdapter;
import io.euphoria.xkcd.app.impl.ui.views.UserListAdapter;

/** Created by Xyzzy on 2021-03-23. */

public class RoomState implements MessageListAdapter.MessageListState, UserListAdapter.UserListState {

    private MessageForest messages;
    private SparseArray<Parcelable> inputBarState;
    private UserList users;

    @Override
    public MessageForest getMessages() {
        return messages;
    }

    @Override
    public void setMessages(MessageForest messages) {
        this.messages = messages;
    }

    @Override
    public SparseArray<Parcelable> getInputBarState() {
        return inputBarState;
    }

    @Override
    public void setInputBarState(SparseArray<Parcelable> inputBarState) {
        this.inputBarState = inputBarState;
    }

    @Override
    public SparseArray<Parcelable> createInputBarState() {
        return new SparseArray<>();
    }

    @Override
    public UserList getUsers() {
        return users;
    }

    @Override
    public void setUsers(UserList users) {
        this.users = users;
    }

}
