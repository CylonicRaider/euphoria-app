package io.euphoria.xkcd.app.impl.ui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.euphoria.xkcd.app.R;
import io.euphoria.xkcd.app.data.Message;

// TODO input bar
public class MessageListAdapter extends RecyclerView.Adapter<MessageListAdapter.ViewHolder> {

    // For logging
    private static final String TAG = "MessageListAdapter";

    // For view identification
    private static final int MESSAGE = 0;
    private static final int INPUT_BAR = 1;

    // Index of all MessageTree objects
    private final Map<String, MessageTree> allMsgs = new HashMap<>();
    // Index of all MessageTree objects whose parents don't exist (yet)
    private final Map<String, List<MessageTree>> orphans = new HashMap<>();
    // List of all displayed messages
    private final List<MessageTree> msgList = new ArrayList<>();

    // View of the input bar
    private final InputBarView inputBar;
    // MessageTree representation of the input bar
    private final MessageTree inputBarTree;
    // The bar is linked in upon the first moveInputBar and then present forever
    private boolean inputBarPresent;

    public MessageListAdapter(InputBarView inputBar) {
        this.inputBar = inputBar;
        inputBarTree = new MessageTree(null);
        inputBarPresent = false;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
                        moveInputBar(mt.getID());
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
        assert ret != -1 : "Scanning for index of message without visible parent";
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

    public synchronized void add(@NonNull Message message) {
        if (allMsgs.containsKey(message.getID())) {
            // Message already registered -> might get away with an in-place update
            MessageTree mt = allMsgs.get(message.getID());
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
            add(importNewMessage(message));
        }
    }

    public synchronized void add(@NonNull MessageTree mt) {
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
        // Update the parents' ideas of their replies, now that they are there
        if (parID != null) {
            parent.addReply(mt);
            updateWithParents(parent);
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
        // Remove from display list
        int index = msgList.indexOf(mt);
        if (index != -1) {
            int replyCount = mt.countVisibleReplies();
            msgList.subList(index, index + 1 + replyCount).clear();
            notifyItemRangeRemoved(index, 1 + replyCount);
            updateWithParents(parent);
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
        MessageTree oldParent = allMsgs.get(inputBarTree.getParent());
        if (oldParent != null) oldParent.removeReply(inputBarTree);
        List<MessageTree> group = orphans.get(inputBarTree.getParent());
        if (group != null) group.remove(inputBarTree);
        int oldIndex = msgList.indexOf(inputBarTree);
        if (oldIndex != -1) msgList.remove(inputBarTree);
        // Link it back in
        inputBarTree.setParent(newParentID);
        if (newParentID == null) {
            // No parent -> top-level
            notifyItemMovedLenient(oldIndex, msgList.size());
            msgList.add(inputBarTree);
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
        // Here be dragons
        updateWithParents(oldParent);
        updateWithParents(newParent);
        inputBar.setIndent(inputBarTree.getIndent());
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

    public synchronized void toggleCollapse(MessageTree mt, boolean newState) {
        if (mt.isCollapsed() != newState)
            toggleCollapse(mt);
    }

    public synchronized void toggleCollapse(MessageTree mt) {
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

        public ViewHolder(View v) {
            super(v);
        }

    }

}
