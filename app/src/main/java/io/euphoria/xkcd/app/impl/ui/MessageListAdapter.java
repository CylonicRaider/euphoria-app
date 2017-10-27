package io.euphoria.xkcd.app.impl.ui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.euphoria.xkcd.app.R;
import io.euphoria.xkcd.app.data.Message;

// TODO input bar
public class MessageListAdapter extends RecyclerView.Adapter<MessageListAdapter.ViewHolder> {

    public interface InputBarListener {

        void onInputBarMoved(String oldParent, String newParent);

    }

    public enum InputBarDirection {
        UP, DOWN, LEFT, RIGHT, ROOT
    }

    // For logging
    private static final String TAG = "MessageListAdapter";

    // For view identification
    private static final int MESSAGE = 0;
    private static final int INPUT_BAR = 1;

    // Index of all MessageTree objects
    private final Map<String, MessageTree> allMsgs = new HashMap<>();
    // Index of all top-level MessageTree objects
    private final List<MessageTree> rootMsgs = new ArrayList<>();
    // Index of all MessageTree objects whose parents don't exist (yet)
    private final Map<String, List<MessageTree>> orphans = new HashMap<>();
    // List of all displayed messages
    private final List<MessageTree> msgList = new ArrayList<>();

    // View of the input bar
    private final InputBarView inputBar;
    // MessageTree representation of the input bar
    private final MessageTree inputBarTree;
    // The bar is linked in upon the first moveInputBar and then present until removed manually
    private boolean inputBarPresent;

    private InputBarListener inputBarListener;

    public MessageListAdapter(InputBarView inputBar) {
        this.inputBar = inputBar;
        inputBarTree = new MessageTree(null);
        inputBarPresent = false;
        inputBar.setMessage(inputBarTree);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        switch (viewType) {
            case MESSAGE:
                MessageView mc = (MessageView) inflater.inflate(R.layout.template_message, null);
                mc.setVisibility(View.INVISIBLE);
                return new ViewHolder(mc);
            case INPUT_BAR:
                inputBar.setVisibility(View.INVISIBLE);
                return new ViewHolder(inputBar);
            default:
                throw new IllegalArgumentException("Unknown view type: " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case MESSAGE:
                MessageView mc = (MessageView) holder.itemView;
                mc.recycle();
                final MessageTree mt = msgList.get(position);
                mc.setMessage(mt);
                mc.setTextClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        moveInputBarAround(mt);
                    }
                });
                mc.setCollapserClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        toggleCollapse(mt);
                    }
                });
                mc.setVisibility(View.VISIBLE);
                break;
            case INPUT_BAR:
                InputBarView ib = (InputBarView) holder.itemView;
                ib.setIndent(msgList.get(position).getIndent());
                ib.setVisibility(View.VISIBLE);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return msgList.size();
    }

    @Override
    public int getItemViewType(int position) {
        MessageTree mt = msgList.get(position);
        return (mt.getMessage() == null) ? INPUT_BAR : MESSAGE;
    }

    public InputBarListener getInputBarListener() {
        return inputBarListener;
    }

    public void setInputBarListener(InputBarListener listener) {
        this.inputBarListener = listener;
    }

    private void dispatchInputBarMoved(String oldParentID, String newParentID) {
        if (inputBarListener != null) inputBarListener.onInputBarMoved(oldParentID, newParentID);
    }

    private List<MessageTree> getParents(@NonNull MessageTree mt) {
        List<MessageTree> ret = new ArrayList<>();
        while (mt.getParent() != null) {
            MessageTree parent = allMsgs.get(mt.getParent());
            ret.add(parent);
            if (parent == null) break;
            mt = parent;
        }
        return ret;
    }

    private boolean isVisible(MessageTree mt) {
        String parID = mt.getParent();
        if (parID == null) return !mt.isCollapsed();
        MessageTree parent = allMsgs.get(parID);
        return parent != null && !parent.isCollapsed() && isVisible(parent);
    }

    private MessageTree importNewMessage(Message message) {
        MessageTree mt = new MessageTree(message);
        if (mt.getID() != null) allMsgs.put(mt.getID(), mt);
        String parID = mt.getParent();
        if (parID != null && allMsgs.get(parID) == null) {
            List<MessageTree> group = orphans.get(parID);
            if (group == null) {
                group = new LinkedList<>();
                orphans.put(parID, group);
            }
            group.add(mt);
        }
        List<MessageTree> adopted = orphans.remove(mt.getID());
        if (adopted != null) {
            mt.addReplies(adopted);
        }
        return mt;
    }

    private int rootInsertPosition(MessageTree mt) {
        int ret = 0;
        while (ret < msgList.size()) {
            MessageTree t = msgList.get(ret);
            if (t.compareTo(mt) >= 0) break;
            ret += 1 + t.countVisibleReplies();
        }
        return ret;
    }

    private int insertPosition(MessageTree mt, MessageTree parent) {
        int ret = msgList.indexOf(parent);
        assert ret != -1 : "Scanning for index for message without visible parent";
        // Skip parent.
        ret++;
        for (MessageTree t : parent.getReplies()) {
            if (t.compareTo(mt) >= 0) break;
            ret += 1 + t.countVisibleReplies();
        }
        return ret;
    }

    private void updateWithParents(MessageTree mt) {
        while (mt != null) {
            // TODO optimize with some reverse index
            notifyItemChanged(msgList.indexOf(mt));
            mt = allMsgs.get(mt.getParent());
        }
    }

    private void notifyItemMovedLenient(int from, int to) {
        if (from == -1 && to == -1) {
            /* NOP */
        } else if (from == -1) {
            notifyItemInserted(to);
        } else if (to == -1) {
            notifyItemRemoved(from);
        } else {
            notifyItemMoved(from, to);
        }
    }

    public synchronized MessageTree getTree(Message message) {
        if (message == null) return inputBarTree;
        return allMsgs.get(message.getID());
    }

    public synchronized MessageTree getParent(@NonNull MessageTree tree) {
        if (tree.getParent() == null) return null;
        MessageTree parent = allMsgs.get(tree.getParent());
        if (parent == null) throw new IllegalArgumentException("Retrieving parent of orphaned message");
        return parent;
    }

    public synchronized MessageTree getSibling(@NonNull MessageTree tree, int offset) {
        List<MessageTree> l;
        if (tree.getParent() == null) {
            l = rootMsgs;
        } else {
            String parID = tree.getParent();
            MessageTree parent = allMsgs.get(parID);
            if (parent != null) {
                l = parent.getReplies();
            } else {
                l = orphans.get(tree.getParent());
                // Ensure the code below fails with the right exception
                if (l == null) l = Collections.emptyList();
            }
        }
        int idx = l.indexOf(tree);
        if (idx == -1)
            throw new IllegalStateException(
                    offset > 0 ? "Retrieving successor of orphaned message" :
                            offset < 0 ? "Retrieving predecessor of orphaned message" :
                                    "Retrieving orphaned message");
        idx += offset;
        if (idx < 0 || idx >= l.size()) return null;
        return l.get(idx);
    }

    public MessageTree getPredecessor(@NonNull MessageTree tree) {
        return getSibling(tree, -1);
    }

    public MessageTree getSuccessor(@NonNull MessageTree tree) {
        return getSibling(tree, 1);
    }

    public MessageTree getReply(@NonNull MessageTree tree, int index) {
        List<MessageTree> l = tree.getReplies();
        if (index < 0) index = l.size() + index;
        if (index < 0 || index >= l.size()) return null;
        return l.get(index);
    }

    public MessageTree getFirstReply(@NonNull MessageTree tree) {
        return getReply(tree, 1);
    }

    public MessageTree getLastReply(@NonNull MessageTree tree) {
        return getReply(tree, -1);
    }

    public synchronized MessageTree add(@NonNull Message message) {
        MessageTree mt = allMsgs.get(message.getID());
        if (mt != null) {
            // Message already registered -> might get away with an in-place update
            mt.setMessage(message);
            int index = msgList.indexOf(mt);
            if (index != -1) {
                // Message already visible -> update in-place
                notifyItemChanged(index);
            } else {
                // Message not visible yet -> perform full-blown import
                add(mt);
            }
        } else {
            // Message not registered -> perform full import
            mt = importNewMessage(message);
            add(mt);
        }
        return mt;
    }

    private synchronized void add(MessageTree mt) {
        String parID = mt.getParent();
        MessageTree parent = allMsgs.get(parID);
        // Scan for insertion position
        int insertIndex;
        if (parID == null) {
            // Top-level message -> employ scanner method
            insertIndex = rootInsertPosition(mt);
        } else if (parent == null || parent.isCollapsed() || !isVisible(parent)) {
            // Message not visible -> nothing to do
            return;
        } else {
            // Visible reply -> employ scanner method
            insertIndex = insertPosition(mt, parent);
        }
        // Check whether message is already there
        if (insertIndex < msgList.size() && mt.equals(msgList.get(insertIndex)))
            return;
        // Insert message along with its visible replies
        List<MessageTree> toInsert = mt.traverseVisibleReplies();
        toInsert.add(0, mt);
        msgList.addAll(insertIndex, toInsert);
        notifyItemRangeInserted(insertIndex, toInsert.size());
        if (parID != null) {
            // Parent existent -> add to its replies
            parent.addReply(mt);
            updateWithParents(parent);
        } else {
            // No parent -> bisect rootMsgs
            int rootIndex = Collections.binarySearch(rootMsgs, mt);
            assert rootIndex < 0 : "Adding already-present message again?!";
            rootMsgs.add(-rootIndex - 1, mt);
        }
    }

    public synchronized void remove(@NonNull MessageTree mt) {
        // Unlink from data structures
        String parID = mt.getParent();
        MessageTree parent = allMsgs.get(mt.getParent());
        if (parent != null) parent.removeReply(mt);
        allMsgs.remove(mt.getID());
        if (!mt.getReplies().isEmpty())
            orphans.put(mt.getID(), new LinkedList<>(mt.getReplies()));
        rootMsgs.remove(mt);
        // Remove from display list
        int index = msgList.indexOf(mt);
        if (index != -1) {
            int replyCount = mt.countVisibleReplies();
            msgList.subList(index, index + 1 + replyCount).clear();
            notifyItemRangeRemoved(index, 1 + replyCount);
            updateWithParents(parent);
        }
        // Special case
        if (mt == inputBarTree) inputBarPresent = false;
    }

    // FIXME: Needs a better name.
    public synchronized void moveInputBarAround(@NonNull MessageTree mt) {
        String preferredID, alternateID;
        if (mt.getParent() == null) {
            preferredID = mt.getID();
            alternateID = null;
        } else {
            preferredID = mt.getParent();
            alternateID = mt.getID();
        }
        if (!inputBarPresent || !preferredID.equals(inputBarTree.getParent())) {
            moveInputBar(preferredID);
        } else {
            moveInputBar(alternateID);
        }
    }

    public synchronized void moveInputBar(String newParentID) {
        // Already there -> nothing to do
        if (inputBarPresent && (newParentID == null ? inputBarTree.getParent() == null :
                newParentID.equals(inputBarTree.getParent())))
            return;
        inputBarPresent = true;
        // Attempt to make the new parent visible
        MessageTree newParent = allMsgs.get(newParentID);
        if (newParent != null) tryEnsureVisible(newParent, true);
        // Remove input bar from data structures
        String oldParentID = inputBarTree.getParent();
        MessageTree oldParent = allMsgs.get(oldParentID);
        if (oldParent != null) oldParent.removeReply(inputBarTree);
        List<MessageTree> group = orphans.get(inputBarTree.getParent());
        if (group != null) group.remove(inputBarTree);
        int oldIndex = msgList.indexOf(inputBarTree);
        if (oldIndex != -1) msgList.remove(inputBarTree);
        int rml = rootMsgs.size() - 1;
        if (rml >= 0 && rootMsgs.get(rml) == inputBarTree) rootMsgs.remove(rml);
        // Link it back in
        inputBarTree.setParent(newParentID);
        if (newParentID == null) {
            // No parent -> top-level
            notifyItemMovedLenient(oldIndex, msgList.size());
            inputBarTree.updateIndent(0);
            msgList.add(inputBarTree);
            rootMsgs.add(inputBarTree);
        } else if (newParent == null) {
            // Parent absent -> orphaned
            notifyItemMovedLenient(oldIndex, -1);
            group = orphans.get(newParentID);
            if (group == null) {
                group = new LinkedList<>();
                orphans.put(newParentID, group);
            }
            group.add(inputBarTree);
            updateWithParents(oldParent);
            dispatchInputBarMoved(oldParentID, newParentID);
            return;
        } else if (!isVisible(newParent)) {
            // Parent not visible -> link in but do not move
            notifyItemMovedLenient(oldIndex, -1);
            newParent.addReply(inputBarTree);
        } else {
            // Parent there and visible -> all fine
            assert !newParent.isCollapsed() : "Failed to uncollapse visible input bar parent?!";
            newParent.addReply(inputBarTree);
            int insertIndex = msgList.indexOf(newParent) + newParent.countVisibleReplies();
            msgList.add(insertIndex, inputBarTree);
            notifyItemMovedLenient(oldIndex, insertIndex);
        }
        updateWithParents(oldParent);
        updateWithParents(newParent);
        inputBar.setIndent(inputBarTree.getIndent());
        dispatchInputBarMoved(oldParentID, newParentID);
    }

    public synchronized boolean navigateInputBar(@NonNull InputBarDirection dir) {
        if (!inputBarPresent) throw new IllegalStateException("Input bar not mounted");
        switch (dir) {
            case UP: // Predecessor of input bar or of closest parent to have one
                MessageTree node = inputBarTree;
                do {
                    MessageTree pred = getPredecessor(node);
                    if (pred != null) {
                        moveInputBar(pred.getID());
                        return true;
                    }
                    node = getParent(node);
                } while (node != null);
                return false;
            case DOWN: // Most deeply nested first child of parent's successor, else parent
                if (inputBarTree.getParent() == null) return false;
                MessageTree par = getParent(inputBarTree);
                MessageTree succ = getSuccessor(par);
                if (succ != null) {
                    // <n00b> cannot use infinite for loop idiom because of auto-formatter ;-; </noob>
                    while (true) {
                        MessageTree child = getFirstReply(succ);
                        if (child == null) break;
                        succ = child;
                    }
                    moveInputBar(succ.getID());
                } else {
                    moveInputBar(par.getParent());
                }
                return true;
            case LEFT: // The parent's parent
                if (inputBarTree.getParent() == null) return false;
                MessageTree parent = getParent(inputBarTree);
                moveInputBar(parent.getParent());
                return true;
            case RIGHT: // The immediate predecessor
                MessageTree pred = getPredecessor(inputBarTree);
                if (pred == null) return false;
                moveInputBar(pred.getID());
                return true;
            case ROOT: // Just the root thread
                moveInputBar(null);
                return true;
        }
        return false;
    }

    public synchronized boolean tryEnsureVisible(@NonNull MessageTree mt, boolean expand) {
        // Determine whether mt is an orphan
        List<MessageTree> parents = getParents(mt);
        int last = parents.size() - 1;
        if (last != -1 && parents.get(last) == null) return false;
        // Find the outermost collapsed parent
        int collapsed;
        for (collapsed = last; collapsed >= 0; collapsed--) {
            if (parents.get(collapsed).isCollapsed()) break;
        }
        // No collapsed parents -> already done
        if (collapsed == -1) {
            if (expand) toggleCollapse(mt, false);
            return true;
        }
        // Expand the nodes
        if (expand) mt.setCollapsed(false);
        for (int i = 0; i < collapsed; i++) parents.get(i).setCollapsed(false);
        toggleCollapse(parents.get(collapsed), false);
        return true;
    }

    public synchronized void toggleCollapse(@NonNull MessageTree mt, boolean newState) {
        if (mt.isCollapsed() != newState)
            toggleCollapse(mt);
    }

    public synchronized void toggleCollapse(@NonNull MessageTree mt) {
        int index = msgList.indexOf(mt);
        assert index != -1 : "Attempting to toggle invisible message";
        if (mt.isCollapsed()) {
            // Un-collapse message for reply traversal
            mt.setCollapsed(false);
            // Message is collapsed -> splice replies back in
            List<MessageTree> toInsert = mt.traverseVisibleReplies();
            msgList.addAll(index + 1, toInsert);
            notifyItemRangeInserted(index + 1, toInsert.size());
        } else {
            // Message is not collapsed -> cut replies out
            int replyCount = mt.countVisibleReplies();
            msgList.subList(index + 1, index + 1 + replyCount).clear();
            notifyItemRangeRemoved(index + 1, replyCount);
            // Collapse after replies have been traversed
            mt.setCollapsed(true);
        }
        updateWithParents(mt);
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        public final BaseMessageView itemMessageView;

        public ViewHolder(BaseMessageView v) {
            super(v);
            itemMessageView = v;
        }

    }

}
