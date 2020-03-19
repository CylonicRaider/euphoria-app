package io.euphoria.xkcd.app.impl.ui;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author N00bySumairu
 */

public class MessageTree implements Comparable<MessageTree> {

    private final List<MessageTree> replies = new ArrayList<>();
    private String id;
    private String parent;
    private UIMessage message;
    private int indent = 0;
    private boolean collapsed = false;

    public MessageTree(UIMessage m) {
        message = m;
        if (m != null) {
            id = m.getID();
            parent = m.getParent();
        }
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MessageTree && compareTo((MessageTree) o) == 0;
    }

    @Override
    public String toString() {
        if (id == null) return String.format("%s@%x[%s/-]", getClass().getSimpleName(), hashCode(), parent);
        return String.format("%s@%x[%s/%s]", getClass().getSimpleName(), hashCode(), parent, id);
    }

    @Override
    public int compareTo(@NonNull MessageTree o) {
        // The input bar is "greater" than every other messgae so that it gets to the bottom
        return id == null ? (o.id == null ? 0 : 1) : (o.id == null ? -1 : id.compareTo(o.id));
    }

    /** The ID of this MessageTree (null for the input bar). */
    public String getID() {
        return id;
    }

    /** The parent node of this MessageTree. */
    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        if (message != null) throw new IllegalStateException("Attempting to change parent of message");
        this.parent = parent;
    }

    /** Sorted list of the immediate replies of this tree. */
    public List<MessageTree> getReplies() {
        return Collections.unmodifiableList(replies);
    }

    /** The message wrapped by this; null for the input bar. */
    public UIMessage getMessage() {
        return message;
    }

    public void setMessage(@NonNull UIMessage m) {
        assert message.getID().equals(id) : "Updating MessageTree with unrelated message";
        message = m;
        id = m.getID();
        parent = m.getParent();
    }

    /** The indentation level of this. */
    public int getIndent() {
        return indent;
    }

    protected void updateIndent(int i) {
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
        if (message == null) throw new IllegalStateException("Input bar cannot have replies");
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
        // TODO replace with bulk insert, sort, and deduplication?
        for (MessageTree t : list) addReply(t);
    }

    /** Remove a MessageTree from the replies list. */
    public void removeReply(@NonNull MessageTree t) {
        replies.remove(t);
    }

    /**
     * Return the amount of visible replies to this MessageTree.
     * A MessageTree is visible iff it has no parent or its parent is visible and not collapsed.
     * The message this method is invoked upon is assumed not to have a parent.
     */
    public int countVisibleReplies() {
        if (collapsed) return 0;
        int ret = 0;
        for (MessageTree mt : replies) {
            ret += 1 + mt.countVisibleReplies();
        }
        return ret;
    }

    /**
     * Count replies for user display
     * If override is true, this and only this MessageTree is assumed to be visible regardless of its actual state.
     * The input bar does not count; otherwise equivalent to countVisibleReplies().
     */
    public int countVisibleUserReplies(boolean override) {
        if (!override && collapsed) return 0;
        int ret = 0;
        for (MessageTree mt : replies) {
            if (mt.getID() == null) continue;
            ret += 1 + mt.countVisibleUserReplies(false);
        }
        return ret;
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
