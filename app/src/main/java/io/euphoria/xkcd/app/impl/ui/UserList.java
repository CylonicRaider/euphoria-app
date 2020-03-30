package io.euphoria.xkcd.app.impl.ui;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.euphoria.xkcd.app.data.SessionView;

/** Created by Xyzzy on 2020-03-29. */

public class UserList implements Parcelable {

    public static class UIUser implements Comparable<UIUser> {

        private final String sessionID;
        private final String agentID;
        private String nickname;

        public UIUser(String sessionID, String agentID, String nickname) {
            this.sessionID = sessionID;
            this.agentID = agentID;
            this.nickname = nickname;
        }

        protected UIUser(Parcel in) {
            sessionID = in.readString();
            agentID = in.readString();
            nickname = in.readString();
        }

        protected void writeToParcel(Parcel dest) {
            dest.writeString(sessionID);
            dest.writeString(agentID);
            dest.writeString(nickname);
        }

        @Override
        public int hashCode() {
            return agentID.hashCode() << 4 ^ nickname.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof UIUser && compareTo((UIUser) obj) == 0;
        }

        @Override
        public String toString() {
            return String.format((Locale) null, "%s@%h[%s|%s/%s]", getClass().getSimpleName(), this,
                    nickname, agentID, sessionID);
        }

        @Override
        public int compareTo(UIUser o) {
            int res = nickname.compareTo(o.nickname);
            if (res != 0) return res;
            return agentID.compareTo(o.agentID);
        }

        public String getSessionID() {
            return sessionID;
        }

        public String getAgentID() {
            return agentID;
        }

        public String getNickname() {
            return nickname;
        }

        private void setNickname(String nickname) {
            this.nickname = nickname;
        }

        private void updateFrom(UIUser other) {
            nickname = other.nickname;
        }

    }

    public static final Creator<UserList> CREATOR = new Creator<UserList>() {

        @Override
        public UserList createFromParcel(Parcel in) {
            return new UserList(in);
        }

        @Override
        public UserList[] newArray(int size) {
            return new UserList[size];
        }

    };


    private final Map<String, UIUser> allUsers;
    private final List<UIUser> displayed;
    private DisplayListener listener;

    public UserList() {
        allUsers = new HashMap<>();
        displayed = new ArrayList<>();
        listener = DisplayListenerAdapter.NULL;
    }

    protected UserList(Parcel in) {
        this();
        int length = in.readInt();
        for (int i = 0; i < length; i++) {
            add(new UIUser(in));
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(allUsers.size());
        for (UIUser item : allUsers.values()) {
            item.writeToParcel(out);
        }
    }

    public DisplayListener getListener() {
        return listener;
    }

    public void setDisplayListener(DisplayListener listener) {
        if (listener == null) listener = DisplayListenerAdapter.NULL;
        this.listener = listener;
    }

    public int size() {
        return displayed.size();
    }

    public UIUser get(int index) {
        return displayed.get(index);
    }

    public boolean has(String sid) {
        return allUsers.containsKey(sid);
    }

    public UIUser get(String sid) {
        return allUsers.get(sid);
    }

    public UIUser add(UIUser usr) {
        if (has(usr.getSessionID())) {
            UIUser existing = get(usr.getSessionID());
            processUpdate(existing, usr);
            return existing;
        } else {
            processInsert(usr);
            return usr;
        }
    }

    public UIUser add(SessionView sess) {
        return add(new UIUser(sess.getSessionID(), sess.getAgentID(), sess.getName()));
    }

    public void setNick(UIUser usr, String newName) {
        UIUser existing = get(usr.getSessionID());
        if (existing == null) throw new IllegalStateException("Trying to rename non-existent user " + usr);
        if (existing.getNickname().equals(newName)) return;
        processRename(usr, newName);
    }

    public UIUser remove(UIUser usr) {
        UIUser existing = get(usr.getSessionID());
        if (existing == null) return null;
        processRemove(existing);
        return existing;
    }

    public void addAll(List<SessionView> users) {
        for (SessionView sv : users) add(sv);
    }

    public void removeAll(List<SessionView> users) {
        for (SessionView sv : users) {
            UIUser item = get(sv.getSessionID());
            if (item != null) remove(item);
        }
    }

    protected void processInsert(UIUser usr) {
        allUsers.put(usr.getSessionID(), usr);
        listener.notifyItemRangeInserted(UIUtils.insertSorted(displayed, usr), 1);
    }

    protected void processUpdate(UIUser usr, UIUser update) {
        usr.updateFrom(update);
        listener.notifyItemChanged(Collections.binarySearch(displayed, usr));
    }

    protected void processRename(UIUser usr, String newName) {
        int oldIndex = Collections.binarySearch(displayed, usr);
        usr.setNickname(newName);
        int newIndex = Collections.binarySearch(displayed, usr);
        if (newIndex < 0) newIndex = -newIndex - 1;
        if (newIndex == oldIndex) return;
        if (newIndex > oldIndex) newIndex -= 1;
        displayed.remove(oldIndex);
        displayed.add(newIndex, usr);
        listener.notifyItemMoved(oldIndex, newIndex);
    }

    protected void processRemove(UIUser usr) {
        allUsers.remove(usr.getSessionID());
        listener.notifyItemRangeRemoved(UIUtils.removeSorted(displayed, usr), 1);
    }

}
