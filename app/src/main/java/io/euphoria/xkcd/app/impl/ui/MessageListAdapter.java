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
public class MessageListAdapter extends RecyclerView.Adapter<MessageListAdapter.MessageViewHolder> {

    // For logging
    private static final String TAG = "MessageListAdapter";

    // Index of all MessageTree objects
    private Map<String, MessageTree> allMsgs = new HashMap<>();
    // Index of all MessageTree objects whose parents don't exist (yet)
    private Map<String, List<MessageTree>> orphans = new HashMap<>();
    // List of all displayed messages
    private List<MessageTree> msgList = new ArrayList<>();

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        MessageContainer mc = (MessageContainer) inflater.inflate(R.layout.template_message, null);
        mc.setVisibility(View.INVISIBLE);
        return new MessageViewHolder(mc);
    }

    @Override
    public void onBindViewHolder(final MessageViewHolder holder, int position) {
        MessageContainer mc = (MessageContainer) holder.itemView;
        mc.recycle();
        mc.setMessage(msgList.get(position));
        mc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleCollapse(msgList.get(holder.getAdapterPosition()));
            }
        });
        mc.setVisibility(View.VISIBLE);
    }

    @Override
    public int getItemCount() {
        return msgList.size();
    }

    private boolean isVisible(MessageTree mt) {
        String parID = mt.getMessage().getParent();
        if (parID == null) return !mt.isCollapsed();
        MessageTree parent = allMsgs.get(parID);
        return parent != null && !parent.isCollapsed() && isVisible(parent);
    }

    private MessageTree importNewMessage(Message message) {
        MessageTree mt = new MessageTree(message);
        allMsgs.put(message.getID(), mt);
        String parID = mt.getMessage().getParent();
        if (parID != null && allMsgs.get(parID) == null) {
            List<MessageTree> group = orphans.get(parID);
            if (group == null) {
                group = new LinkedList<>();
                orphans.put(parID, group);
            }
            group.add(mt);
        }
        List<MessageTree> adopted = orphans.remove(parID);
        if (adopted != null) {
            mt.addReplies(adopted);
        }
        return mt;
    }

    private int rootInsertPosition(MessageTree mt) {
        int ret = 0;
        while (ret < msgList.size()) {
            MessageTree t = msgList.get(ret);
            if (t.getMessage().getID().compareTo(mt.getMessage().getID()) >= 0) break;
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
            if (t.getMessage().getID().compareTo(mt.getMessage().getID()) >= 0) break;
            ret += 1 + t.countVisibleReplies();
        }
        return ret;
    }

    public synchronized void add(@NonNull Message message) {
        if (allMsgs.containsKey(message.getID())) {
            // Message already exists -> update in-place
            MessageTree mt = allMsgs.get(message.getID());
            mt.setMessage(message);
            notifyItemChanged(msgList.indexOf(mt));
        } else {
            // Message is new -> import into data structures
            MessageTree mt = importNewMessage(message);
            String parID = message.getParent();
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
            // Insert message along with its visible replies
            List<MessageTree> toInsert = mt.traverseVisibleReplies();
            toInsert.add(0, mt);
            msgList.addAll(insertIndex, toInsert);
            notifyItemRangeInserted(insertIndex, toInsert.size());
            // Update the parent's idea of its replies, now that they are there
            if (parID != null) parent.addReply(mt);
        }
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
    }

    class MessageViewHolder extends RecyclerView.ViewHolder {

        public MessageViewHolder(MessageContainer mc) {
            super(mc);
        }

    }
}
