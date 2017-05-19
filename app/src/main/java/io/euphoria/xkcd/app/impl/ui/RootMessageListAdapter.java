package io.euphoria.xkcd.app.impl.ui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.euphoria.xkcd.app.R;
import io.euphoria.xkcd.app.data.Message;

/**
 * @author N00bySumairu
 */

// TODO
public class RootMessageListAdapter extends RecyclerView.Adapter<RootMessageListAdapter.MessageViewHolder> {

    private List<MessageTree> topLevelMsgs = new ArrayList<>();
    private Map<String, MessageTree> allMsgs = new HashMap<>();

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        MessageContainer mc = (MessageContainer) inflater.inflate(R.layout.template_message, null);
        return new MessageViewHolder(mc);
    }

    @Override
    public void onBindViewHolder(MessageViewHolder holder, int position) {
        MessageContainer mc = ((MessageContainer) holder.itemView);
        mc.recycle();
        mc.setMessage(topLevelMsgs.get(position));
        LinearLayout ll = (LinearLayout) mc.findViewById(R.id.reply_list);
        for (MessageTree subTree : topLevelMsgs.get(position).subTrees) {
            ll.addView(getPopulatedMC(ll, subTree));
        }
    }

    @Override
    public int getItemCount() {
        return topLevelMsgs.size();
    }

    private MessageContainer getPopulatedMC(ViewGroup parent, MessageTree message) {
        MessageContainer mc = prepareNewMC(parent, message);
        LinearLayout ll = (LinearLayout) mc.findViewById(R.id.reply_list);
        for (Map.Entry<String, MessageTree> entry : allMsgs.entrySet()) {
            if (message.getID().equals(entry.getValue().getParent())) {
                ll.addView(prepareNewMC(ll, entry.getValue()));
            }
        }
        return mc;
    }

    private MessageContainer prepareNewMC(ViewGroup parent, MessageTree message) {
        LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        MessageContainer mc = (MessageContainer) inflater.inflate(R.layout.template_message, null);
        mc.setMessage(message);
        return mc;
    }

    /**
     * Returns the position of a Message in the List of all Messages,
     * where the position is the position of the top level message,
     * that is the passed messages root.
     *
     * @param message Message to get index of
     * @return Index of message
     */
    public int indexOf(@NonNull Message message) {
        Message tmp = message;
        while (tmp.getParent() != null) {
            tmp = allMsgs.get(tmp.getParent());
        }
        for (int i = 0; i < topLevelMsgs.size(); i++) {
            if (topLevelMsgs.get(i).getID().equals(tmp.getID())) {
                return i;
            }
        }
        return -1;
    }

    public void add(@NonNull Message message) {
        if (allMsgs.containsKey(message.getID())) {
            allMsgs.get(message.getID()).updateMessage(message);
            notifyItemChanged(indexOf(message));
        } else {
            if (message.getParent() == null) {
                MessageTree mt = new MessageTree(message);
                topLevelMsgs.add(mt);
                allMsgs.put(message.getID(), mt);
                notifyItemInserted(topLevelMsgs.size() - 1);
            } else {
                MessageTree mt = new MessageTree(message);
                allMsgs.get(message.getParent()).addSubTree(mt);
                allMsgs.put(message.getID(), mt);
                notifyItemChanged(indexOf(message));
            }
        }
    }

    class MessageViewHolder extends RecyclerView.ViewHolder {

        public MessageViewHolder(MessageContainer mc) {
            super(mc);
        }
    }
}
