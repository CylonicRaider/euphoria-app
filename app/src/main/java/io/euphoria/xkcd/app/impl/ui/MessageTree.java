package io.euphoria.xkcd.app.impl.ui;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import io.euphoria.xkcd.app.data.Message;

/**
 * @author N00bySumairu
 */

public class MessageTree implements Comparable<MessageTree> {

    private final List<MessageTree> replies = new ArrayList<>();
    private Message message;
    private int indent = 0;
    private boolean collapsed = false;

    public MessageTree(@NonNull Message m) {
        message = m;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MessageTree && message.getID().equals(((MessageTree) o).getMessage().getID());
    }

    public String toString() {
        return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + "[id=" + getMessage().getID() +
                "]";
    }

    @Override
    public int compareTo(@NonNull MessageTree o) {
        return message.getID().compareTo(o.getMessage().getID());
    }

    /** Sorted list of the immediate replies of this tree. */
    public List<MessageTree> getReplies() {
        return Collections.unmodifiableList(replies);
    }

    /** The message wrapped by this. */
    public Message getMessage() {
        return message;
    }

    public void setMessage(@NonNull Message m) {
        assert message.getID().equals(m.getID()) : "Updating MessageTree with unrelated message";
        message = m;
    }

    /** The indentation level of this. */
    public int getIndent() {
        return indent;
    }

    protected void updateIndent(int i) {
        // TODO profile for performance
        indent = i++;
        for (MessageTree t : replies) t.updateIndent(i);
    }

    /** Whether the <em>replies</em> of this are invisible. */
    public boolean isCollapsed() {
        return collapsed;
    }

    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
    }

    /** Add a MessageTree to the replies list. */
    public void addReply(@NonNull MessageTree t) {
        t.updateIndent(indent + 1);
        int idx = Collections.binarySearch(replies, t);
        if (idx >= 0) {
            replies.set(idx, t);
        } else {
            replies.add(-idx - 1, t);
        }
    }

    /** Add every MessageTree in the list as a reply. */
    public void addReplies(@NonNull Collection<MessageTree> list) {
        // TODO replace with bulk insert, sort, and dedplication?
        for (MessageTree t : list) addReply(t);
    }

    /**
     * Return the amount of visible replies to this MessageTree.
     * If override is true, this (and only this) MessageTree is assumed not to be collapsed.
     * A MessageTree is visible iff it has no parent or its parent is visible and not collapsed.
     * The message this method is invoked upon is assumed not to have a parent.
     */
    public int countVisibleReplies(boolean override) {
        if (!override && collapsed) return 0;
        int ret = 0;
        for (MessageTree mt : replies) {
            ret += 1 + mt.countVisibleReplies(false);
        }
        return ret;
    }

    /** Equivalent to countVisibleReplies(false) */
    public int countVisibleReplies() {
        return countVisibleReplies(false);
    }

    /** Return a list of all the visible replies to this MessageTree. */
    public List<MessageTree> traverseVisibleReplies() {
        List<MessageTree> ret = new ArrayList<>();
        traverseVisible(ret);
        return ret;
    }
    private void traverseVisible(List<MessageTree> drain) {
        if (isCollapsed()) return;
        for (MessageTree t : replies) {
            drain.add(t);
            t.traverseVisible(drain);
        }
    }

}
