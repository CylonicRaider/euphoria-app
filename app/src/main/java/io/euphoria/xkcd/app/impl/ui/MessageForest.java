package io.euphoria.xkcd.app.impl.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/** Created by Xyzzy on 2020-03-19. */

public class MessageForest {

    public interface DisplayListener {

        void notifyItemRangeInserted(int start, int length);

        void notifyItemChanged(int index);

        void notifyItemMoved(int from, int to);

        void notifyItemRangeRemoved(int start, int length);

    }

    public static class DisplayListenerAdapter implements DisplayListener {

        private static DisplayListener NULL = new DisplayListenerAdapter();

        @Override
        public void notifyItemRangeInserted(int start, int length) {}

        @Override
        public void notifyItemChanged(int index) {}

        @Override
        public void notifyItemMoved(int from, int to) {}

        @Override
        public void notifyItemRangeRemoved(int start, int length) {}

    }

    /* All messages by ID. */
    private final Map<String, MessageTree> allMessages;
    /* The roots (i.e. messages with a parent of null) ordered by ID. */
    private final List<MessageTree> roots;
    /* Messages whose parent has not been added yet (mapping from parent ID). */
    private final Map<String, List<MessageTree>> orphans;
    /* The list of all visible messages in their proper order. */
    private final List<MessageTree> displayed;
    /* A listener for structural changes of the display list. */
    private DisplayListener listener;

    public MessageForest() {
        allMessages = new HashMap<>();
        roots = new ArrayList<>();
        orphans = new HashMap<>();
        displayed = new ArrayList<>();
        listener = DisplayListenerAdapter.NULL;
    }

    public DisplayListener getListener() {
        return listener;
    }

    public void setListener(DisplayListener listener) {
        if (listener == null) listener = DisplayListenerAdapter.NULL;
        this.listener = listener;
    }

    public int size() {
        return displayed.size();
    }

    public MessageTree get(int index) {
        return displayed.get(index);
    }

    public boolean has(String id) {
        return allMessages.containsKey(id);
    }

    public MessageTree get(String id) {
        return allMessages.get(id);
    }

    public MessageTree getParent(MessageTree mt) {
        return get(mt.getParent());
    }

    public MessageTree getThreadRoot(MessageTree mt) {
        while (true) {
            String parentID = mt.getParent();
            if (parentID == null) return mt;
            if (!has(parentID)) return null;
            mt = get(parentID);
        }
    }

    public MessageTree getReply(MessageTree mt, int index) {
        List<MessageTree> replies = mt.getReplies();
        if (index < 0) index += replies.size();
        if (index < 0 || index >= replies.size()) return null;
        return replies.get(index);
    }

    public MessageTree getSibling(MessageTree mt, int offset) {
        List<MessageTree> container;
        if (mt.getParent() == null) {
            container = roots;
        } else if (!has(mt.getParent())) {
            container = getOrphanList(mt.getParent());
        } else {
            container = getParent(mt).getReplies();
        }
        int index = Collections.binarySearch(container, mt);
        if (index < 0) throw new NoSuchElementException("Trying to get sibling of non-linked-in MessageTree " + mt);
        index += offset;
        if (index < 0 || index >= container.size()) return null;
        return container.get(index);
    }

    protected List<MessageTree> getOrphanList(String parentID) {
        List<MessageTree> ret = orphans.get(parentID);
        if (ret == null) {
            ret = new ArrayList<>();
            orphans.put(parentID, ret);
        }
        return ret;
    }

    protected int findRootDisplayIndex(MessageTree mt) {
        int index = 0;
        while (index < displayed.size()) {
            MessageTree cur = displayed.get(index);
            if (cur.compareTo(mt) >= 0) break;
            index += 1 + cur.countVisibleReplies();
        }
        return index;
    }

    protected int findDisplayIndex(MessageTree mt, boolean markUpdate) {
        if (mt.getParent() == null) return findRootDisplayIndex(mt);
        MessageTree parent = getParent(mt);
        if (parent == null || parent.isCollapsed()) return -1;
        int index = findDisplayIndex(parent, markUpdate);
        if (index == -1) return -1;
        if (markUpdate) listener.notifyItemChanged(index);
        index++;
        for (MessageTree sib : parent.getReplies()) {
            if (sib.compareTo(mt) >= 0) break;
            index += 1 + sib.countVisibleReplies();
        }
        return index;
    }

    public MessageTree add(MessageTree mt) {
        MessageTree existing = allMessages.get(mt.getID());
        if (existing == null) {
            processInsert(mt);
            return mt;
        } else if (!UIUtils.equalsOrNull(mt.getMessage(), existing.getMessage())) {
            processReplace(mt);
            return mt;
        } else {
            return existing;
        }
    }

    public MessageTree add(UIMessage msg) {
        MessageTree existing = allMessages.get(msg.getID());
        if (existing == null) {
            MessageTree mt = new MessageTree(msg);
            allMessages.put(mt.getID(), mt);
            processInsert(mt);
            return mt;
        } else if (!UIUtils.equalsOrNull(msg, existing.getMessage())) {
            existing.setMessage(msg);
            processReplace(existing);
            return existing;
        } else {
            return existing;
        }
    }

    public void setCollapsed(MessageTree mt, boolean collapsed) {
        if (collapsed == mt.isCollapsed()) return;
        processCollapse(mt, collapsed);
    }

    public void toggleCollapsed(MessageTree mt) {
        processCollapse(mt, !mt.isCollapsed());
    }

    public boolean tryEnsureVisible(MessageTree mt) {
        MessageTree parent = getParent(mt);
        if (mt.getParent() == null) {
            // Roots are always visible.
            return true;
        } else if (parent == null) {
            // Orphans cannot be made visible.
            return false;
        } else {
            // Otherwise, we go to the parent.
            setCollapsed(parent, false);
            return tryEnsureVisible(parent);
        }
    }

    public void move(MessageTree mt, MessageTree newParent, boolean ensureVisible) {
        if (ensureVisible && newParent != null) tryEnsureVisible(newParent);
        String newParentID = (newParent == null) ? null : newParent.getID();
        if (UIUtils.equalsOrNull(mt.getParent(), newParentID)) return;
        processMove(mt, newParent);
    }

    public MessageTree remove(MessageTree mt, boolean recursive) {
        MessageTree existing = allMessages.get(mt.getID());
        if (existing == null) return null;
        processRemove(existing, recursive);
        return existing;
    }

    protected void addDisplayRange(MessageTree mt, int index, boolean includeSelf) {
        List<MessageTree> toAdd = mt.traverseVisibleReplies(includeSelf);
        if (!includeSelf) index++;
        displayed.addAll(index, toAdd);
        listener.notifyItemRangeInserted(index, toAdd.size());
    }

    protected void removeDisplayRange(MessageTree mt, int index, boolean includeSelf) {
        int length = mt.countVisibleReplies();
        if (includeSelf) {
            length++;
        } else {
            index++;
        }
        displayed.subList(index, index + length).clear();
        listener.notifyItemRangeRemoved(index, length);
    }

    protected void notifyItemMovedLenient(int from, int to) {
        if (from == -1 && to == -1) {
            /* NOP */
        } else if (from == -1) {
            listener.notifyItemRangeInserted(to, 1);
        } else if (to == -1) {
            listener.notifyItemRangeRemoved(from, 1);
        } else {
            listener.notifyItemMoved(from, to);
        }
    }

    protected void processInsert(MessageTree mt) {
        allMessages.put(mt.getID(), mt);
        // Adopt orphans! :)
        List<MessageTree> children = orphans.remove(mt.getID());
        if (children != null) mt.addReplies(children);
        // The next steps depend on whether the message has a parent.
        int displayIndex;
        String parentID = mt.getParent();
        if (parentID == null) {
            // If there is no parent, this is a new root.
            UIUtils.insertSorted(roots, mt);
            displayIndex = findRootDisplayIndex(mt);
        } else if (!has(parentID)) {
            // If the parent does not exist, the message is an orphan.
            getOrphanList(parentID).add(mt);
            return;
        } else {
            // Otherwise, a parent exists.
            displayIndex = findDisplayIndex(mt, true);
            get(parentID).addReply(mt);
            // Updating the display list does, naturally, not happen for invisible messages.
            if (displayIndex == -1) return;
        }
        // Finally, splice the message (along with its replies!) into the display list.
        addDisplayRange(mt, displayIndex, true);
    }

    protected void processReplace(MessageTree mt) {
        // If an already-existing MessageTree's underlying message has been replaced, we only need to mark it as
        // changed (and do so for all its parents for good measure).
        int index = findDisplayIndex(mt, true);
        if (index != -1) listener.notifyItemChanged(index);
    }

    protected void processCollapse(MessageTree mt, boolean collapse) {
        // Locate the message, marking its parents for refreshing. If it is invisible, there is little to do.
        int displayIndex = findDisplayIndex(mt, true);
        if (displayIndex == -1) {
            mt.setCollapsed(collapse);
            return;
        }
        // Do not forget to mark the message itself for updating.
        listener.notifyItemChanged(displayIndex);
        // Now to the main branch.
        if (collapse) {
            removeDisplayRange(mt, displayIndex, false);
            mt.setCollapsed(true);
        } else {
            mt.setCollapsed(false);
            addDisplayRange(mt, displayIndex, false);
        }
    }

    protected void processMove(MessageTree mt, MessageTree newParent) {
        // The sequence of operations is somewhat tricky, in particular w.r.t. ensuring we pass the right indices
        // to the update listener.
        // First, unlink the message from the data structures.
        int oldIndex = findDisplayIndex(mt, true);
        if (oldIndex != -1) {
            displayed.remove(oldIndex);
        }
        MessageTree oldParent = getParent(mt);
        if (oldParent != null) {
            oldParent.removeReply(mt);
        } else if (mt.getParent() == null) {
            UIUtils.removeSorted(roots, mt);
        } else {
            List<MessageTree> siblings = orphans.get(mt.getParent());
            if (siblings != null) siblings.remove(mt);
        }
        // Now, flip the message's parent to the new one.
        mt.setParent((newParent == null) ? null : newParent.getID());
        // Next, re-link the message into the data structures.
        // Here, we cannot use findDisplayIndex()' auto-notification feature as we have not yet notified the listener
        // about the message's move.
        int newIndex = findDisplayIndex(mt, false);
        if (newIndex != -1) {
            displayed.add(newIndex, mt);
        }
        if (newParent != null) {
            newParent.addReply(mt);
        } else {
            UIUtils.insertSorted(roots, mt);
        }
        // Finally, issue listener notifications.
        notifyItemMovedLenient(oldIndex, newIndex);
        findDisplayIndex(mt, true);
    }

    protected void processRemove(MessageTree mt, boolean recursive) {
        allMessages.remove(mt.getID());
        if (recursive) {
            // If removing recursively, the children need not become orphans, but have to be removed from the ID index
            // instead.
            for (MessageTree r : mt.traverseVisibleReplies(false)) allMessages.remove(r.getID());
        } else {
            // Create orphans! :(
            getOrphanList(mt.getID()).addAll(mt.getReplies());
        }
        // Remove the message from the root list.
        if (mt.getParent() == null) UIUtils.removeSorted(roots, mt);
        // Locate the message in the display list, and mark its parents for updates.
        int displayIndex = findDisplayIndex(mt, true);
        if (displayIndex == -1) return;
        // Splice the message along with its replies out.
        removeDisplayRange(mt, displayIndex, true);
    }

}
