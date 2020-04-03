package io.euphoria.xkcd.app.impl.ui;

import android.os.Parcel;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author N00bySumairu
 */

public class MessageTree implements Comparable<MessageTree> {

    public static long idStringToLong(String id) {
        if (id.equals(CURSOR_ID)) return -1;
        return Long.parseLong(id, 36);
    }

    public static String idLongToString(long id) {
        if (id == -1) return CURSOR_ID;
        return Long.toString(id, 36);
    }

    private static String formatID(String id) {
        return (id == null) ? "-" : (id.equals(CURSOR_ID)) ? "~" : id;
    }

    public static final String CURSOR_ID = "\uFFFF";

    protected static final byte PF_IS_A_THING  = 0x01;
    protected static final byte PF_HAS_CONTENT = 0x02;
    protected static final byte PF_HAS_REPLIES = 0x04;
    protected static final byte PF_TRUNCATED   = 0x08;
    protected static final byte PF_COLLAPSED   = 0x10;

    private final List<MessageTree> replies = new ArrayList<>();
    private String id;
    private String parent;
    private UIMessage message;
    private long longID;
    private int indent = 0;
    private boolean collapsed = false;

    public MessageTree(UIMessage m) {
        message = m;
        if (m == null) {
            id = CURSOR_ID;
            parent = null;
        } else {
            id = m.getID();
            parent = m.getParent();
        }
        longID = idStringToLong(id);
    }

    protected MessageTree(Parcel in, byte flags, String parent) {
        if ((flags & PF_IS_A_THING) == 0) {
            throw new IllegalArgumentException("Invalid encoded MessageTree flags");
        }
        String id = in.readString();
        if ((flags & PF_HAS_CONTENT) != 0) {
            message = new UIMessage(in, id, parent, ((flags & PF_TRUNCATED) != 0));
        }
        this.id = id;
        this.parent = parent;
        this.longID = idStringToLong(id);
        this.collapsed = ((flags & PF_COLLAPSED) != 0);
        if ((flags & PF_HAS_REPLIES) != 0) {
            while (true) {
                byte nextFlags = in.readByte();
                if (nextFlags == 0) break;
                addReply(new MessageTree(in, nextFlags, id));
            }
        }
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MessageTree && compareTo((MessageTree) o) == 0;
    }

    @Override
    public String toString() {
        return String.format("%s@%x[%s/%s]", getClass().getSimpleName(), hashCode(), formatID(parent), formatID(id));
    }

    @Override
    public int compareTo(@NonNull MessageTree o) {
        return id.compareTo(o.id);
    }

    /** The ID of this MessageTree (CURSOR_ID for the input bar). */
    public String getID() {
        return id;
    }

    /** The parent node of this MessageTree. */
    public String getParent() {
        return parent;
    }

    /**
     * A long containing the numerical value of this message's ID.
     * CURSOR_ID is mapped to the special value -1.
     */
    public long getLongID() {
        return longID;
    }

    /**
     * Set this MessageTree's parent ID.
     * Only permissible for a MessageTree representing the input bar.
     */
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

    /** Change the message wrapped by this MessageTree. */
    public void setMessage(@NonNull UIMessage m) {
        assert m.getID().equals(id) : "Updating MessageTree with unrelated message";
        message = m;
        id = m.getID();
        parent = m.getParent();
        longID = idStringToLong(id);
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

    /** Set the {@link #isCollapsed()} flag. */
    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
    }

    /**
     * Add a MessageTree to the replies list.
     * Returns the index into the replies list at which t now resides.
     */
    public int addReply(@NonNull MessageTree t) {
        if (message == null) throw new IllegalStateException("Input bar cannot have replies");
        t.updateIndent(indent + 1);
        return UIUtils.insertSorted(replies, t);
    }

    /** Add every MessageTree in the list as a reply. */
    public void addReplies(@NonNull Collection<MessageTree> list) {
        // TODO replace with bulk insert, sort, and deduplication?
        for (MessageTree t : list) addReply(t);
    }

    /** Remove a MessageTree from the replies list. */
    public void removeReply(@NonNull MessageTree t) {
        UIUtils.removeSorted(replies, t);
    }

    /**
     * Return the amount of visible replies to this MessageTree.
     * A MessageTree is visible iff it has no parent or its parent is visible and not collapsed.
     * The message this method is invoked upon is assumed not to have a parent.
     */
    public int countVisibleReplies() {
        // TODO cache this?
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
            if (mt.getID().equals(CURSOR_ID)) continue;
            ret += 1 + mt.countVisibleUserReplies(false);
        }
        return ret;
    }

    /** Return a list of all the visible replies to this MessageTree. */
    public List<MessageTree> traverseVisibleReplies(boolean includeThis) {
        List<MessageTree> ret = new ArrayList<>();
        if (includeThis) ret.add(this);
        traverseVisibleRepliesInner(ret);
        return ret;
    }

    private void traverseVisibleRepliesInner(List<MessageTree> drain) {
        if (isCollapsed()) return;
        for (MessageTree t : replies) {
            drain.add(t);
            t.traverseVisibleRepliesInner(drain);
        }
    }

    /** Serialization logic. */
    protected void writeToParcel(Parcel out) {
        byte flags = PF_IS_A_THING;
        if (message != null) {
            flags |= PF_HAS_CONTENT;
            if (message.isTruncated()) flags |= PF_TRUNCATED;
        }
        if (replies.size() != 0) {
            flags |= PF_HAS_REPLIES;
        }
        if (isCollapsed()) {
            flags |= PF_COLLAPSED;
        }
        out.writeByte(flags);
        out.writeString(id);
        if ((flags & PF_HAS_CONTENT) != 0) {
            message.writeToParcel(out);
        }
        if ((flags & PF_HAS_REPLIES) != 0) {
            for (MessageTree mt : replies) mt.writeToParcel(out);
            out.writeByte((byte) 0);
        }
    }

}
