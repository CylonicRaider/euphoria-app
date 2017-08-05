package io.euphoria.xkcd.app.impl.ui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.euphoria.xkcd.app.R;
import io.euphoria.xkcd.app.data.Message;

/**
 * @author N00bySumairu
 */

// TODO remove ghost item at bottom!!
// TODO adjust layout so recyclerview is never behind toolbar!!
// TODO input bar
public class MessageListAdapter extends RecyclerView.Adapter<MessageListAdapter.MessageViewHolder> {

    public static final String TAG = "MessageListAdapter";

    private Map<String, MessageTree> allMsgs = new HashMap<>();
    private List<MessageTree> msgList = Collections.synchronizedList(new ArrayList<MessageTree>());

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        MessageContainer mc = (MessageContainer) inflater.inflate(R.layout.template_message, null);
        mc.setVisibility(View.INVISIBLE);
        return new MessageViewHolder(mc);
    }

    @Override
    public void onBindViewHolder(final MessageViewHolder holder, int position) {
        MessageContainer mc = ((MessageContainer) holder.itemView);
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

    public void add(@NonNull Message message) {
        if (allMsgs.containsKey(message.getID())) {
            MessageTree mt = allMsgs.get(message.getID());
            mt.updateMessage(message);
            notifyItemChanged(msgList.indexOf(mt));
        } else {
            if (message.getParent() == null) {
                MessageTree mt = MessageTree.wrap(message);
                msgList.add(mt);
                allMsgs.put(message.getID(), mt);
                notifyItemInserted(allMsgs.size() - 1);
            } else {
                MessageTree mt = MessageTree.wrap(message);
                MessageTree parentMt = allMsgs.get(mt.getParent());
                if (!parentMt.isCollapsed()) {
                    List<MessageTree> precedingMsgs = parentMt.getReplies();
                    int insertPos;
                    if (precedingMsgs.isEmpty()) {
                        insertPos = msgList.indexOf(parentMt) + 1;
                    } else {
                        insertPos = msgList.indexOf(precedingMsgs.get(precedingMsgs.size() - 1)) + 1;
                    }
                    msgList.add(insertPos, mt);
                    allMsgs.get(message.getParent()).addReply(mt);
                    allMsgs.put(message.getID(), mt);
                    notifyItemInserted(insertPos);
                }
            }
        }
    }

    // TODO not robust!! breaks when messing around (creates duplicate messages)
    public synchronized void toggleCollapse(MessageTree mt) {
        List<MessageTree> replies = mt.getReplies();
        if (!replies.isEmpty()) {
            if (mt.isCollapsed()) {
                int firstReplyI = msgList.indexOf(mt)+1;
                int lastReplyI = recursivelyInsertReplies(mt, firstReplyI-1);
                notifyItemRangeInserted(firstReplyI, lastReplyI-firstReplyI+1);
            } else {
                MessageTree lastReply = mt;
                List<MessageTree> subReplies = lastReply.getReplies();
                do {
                    lastReply = subReplies.get(subReplies.size()-1);
                    subReplies = lastReply.getReplies();
                } while (!subReplies.isEmpty());
                int firstReplyI = msgList.indexOf(mt)+1;
                int lastReplyI = msgList.indexOf(lastReply);
                for (int i = firstReplyI; i <= lastReplyI; i++)
                    msgList.remove(firstReplyI);
                notifyItemRangeRemoved(firstReplyI, lastReplyI-firstReplyI+1);
            }
        }
        mt.setCollapsed(!mt.isCollapsed());
    }

    private int recursivelyInsertReplies(MessageTree mt, int index) {
        Log.d(TAG, "recursivelyInsertReplies: "+mt.getIndent());
        int lastReply = index;
        for (MessageTree reply : mt.getReplies()) {
            msgList.add(lastReply+1, reply);
            lastReply = recursivelyInsertReplies(reply, lastReply+1);
        }
        return lastReply;
    }

    class MessageViewHolder extends RecyclerView.ViewHolder {

        public MessageViewHolder(MessageContainer mc) {
            super(mc);
        }
    }
}
