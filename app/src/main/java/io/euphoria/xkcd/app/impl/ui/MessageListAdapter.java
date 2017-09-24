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

    private final InputBarView inputBar;
    private final MessageTree inputBarTree;

    public MessageListAdapter(InputBarView inputBar) {
        this.inputBar = inputBar;
        inputBarTree = new MessageTree(null);
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
    public void onBindViewHolder(final ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case MESSAGE:
                MessageView mc = (MessageView) holder.itemView;
                mc.recycle();
                mc.setMessage(msgList.get(position));
                // TODO input bar navigation
                mc.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        toggleCollapse(msgList.get(holder.getAdapterPosition()));
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
        if (! mt.getReplies().isEmpty())
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

    public synchronized void moveInputBar(final String newParentID) {
        // Input bar not present -> use normal insertion logic
        int index = msgList.indexOf(inputBarTree);
        if (index == -1) {
            inputBarTree.setParent(newParentID);
            add(inputBarTree);
            return;
        }
        // Already there -> nothing to do
        if (newParentID == null ? inputBarTree.getParent() == null : newParentID.equals(inputBarTree.getParent()))
            return;
        // Unlink from old parent
        MessageTree parent = allMsgs.get(inputBarTree.getParent());
        if (parent != null) parent.removeReply(inputBarTree);
        msgList.remove(index);
        // Assign new parent for both branches
        inputBarTree.setParent(newParentID);
        // New parent not present -> schedule for re-addition
        MessageTree newParent = allMsgs.get(newParentID);
        if (newParent == null) {
            add(inputBarTree);
            notifyItemRemoved(index);
            return;
        }
        // Else -> Link it again
        int insertIndex;
        if (newParentID == null) {
            // ...Does it count as constant folding if it is manual and makes tons of assumptions?
            insertIndex = msgList.size();
        } else {
            insertIndex = msgList.indexOf(newParent) + 1 + newParent.countVisibleReplies();
        }
        msgList.add(insertIndex, inputBarTree);
        newParent.addReply(inputBarTree);
        notifyItemMoved(index, insertIndex);
        updateWithParents(parent);
        updateWithParents(newParent);
        inputBar.setIndent(inputBarTree.getIndent());
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
