package io.euphoria.xkcd.app.impl.ui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.euphoria.xkcd.app.R;
import io.euphoria.xkcd.app.data.Message;

// TODO input bar
public class MessageListAdapter extends RecyclerView.Adapter<MessageListAdapter.ViewHolder>
                                implements DisplayListener {

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

    // The main data structure
    private final MessageForest data;
    // View of the input bar
    private final InputBarView inputBar;
    // MessageTree representation of the input bar
    private final MessageTree inputBarTree;

    private InputBarListener inputBarListener;

    public MessageListAdapter(MessageForest data, InputBarView inputBar) {
        this.data = data;
        this.inputBar = inputBar;
        if (data.has(MessageTree.CURSOR_ID)) {
            inputBarTree = data.get(MessageTree.CURSOR_ID);
        } else {
            inputBarTree = new MessageTree(null);
            data.add(inputBarTree);
        }
        data.setListener(this);
        inputBar.recycle();
        inputBar.setMessage(inputBarTree);
        setHasStableIds(true);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        switch (viewType) {
            case MESSAGE:
                MessageView mc = (MessageView) inflater.inflate(R.layout.template_message, parent, false);
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
                final MessageTree mt = getItem(position);
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
                ib.setIndent(getItem(position).getIndent());
                ib.setVisibility(View.VISIBLE);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public MessageTree getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getLongID();
    }

    @Override
    public int getItemViewType(int position) {
        MessageTree mt = getItem(position);
        return (mt.getMessage() == null) ? INPUT_BAR : MESSAGE;
    }

    public MessageForest getData() {
        return data;
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

    public int indexOf(MessageTree mt) {
        return data.findDisplayIndex(mt, true, false);
    }

    public MessageTree get(String id) {
        return data.get(id);
    }

    public MessageTree getTree(Message message) {
        return get((message == null) ? null : message.getID());
    }

    public MessageTree getParent(@NonNull MessageTree tree) {
        return data.getParent(tree);
    }

    public MessageTree getReply(MessageTree mt, int index) {
        return data.getReply(mt, index);
    }

    public MessageTree getSibling(MessageTree mt, int offset) {
        return data.getSibling(mt, offset);
    }

    public MessageTree add(@NonNull MessageTree mt) {
        return data.add(mt);
    }

    public MessageTree add(@NonNull UIMessage message) {
        return data.add(message);
    }

    public void remove(@NonNull MessageTree mt) {
        data.remove(mt, false);
    }

    // FIXME: Needs a better name.
    public void moveInputBarAround(@NonNull MessageTree mt) {
        String preferredID, alternateID;
        if (mt.getParent() == null) {
            preferredID = mt.getID();
            alternateID = null;
        } else {
            preferredID = mt.getParent();
            alternateID = mt.getID();
        }
        moveInputBar((preferredID.equals(inputBarTree.getParent())) ? alternateID : preferredID);
    }

    public void moveInputBar(String newParentID) {
        String oldParentID = inputBarTree.getParent();
        data.move(inputBarTree, get(newParentID), true);
        inputBar.setIndent(inputBarTree.getIndent());
        dispatchInputBarMoved(oldParentID, newParentID);
    }

    public boolean navigateInputBar(@NonNull InputBarDirection dir) {
        switch (dir) {
            case UP: // Predecessor of input bar or of closest parent to have one
                MessageTree node = inputBarTree;
                do {
                    MessageTree pred = getSibling(node, -1);
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
                MessageTree succ = getSibling(par, 1);
                if (succ != null) {
                    // <n00b> cannot use infinite for loop idiom because of auto-formatter ;-; </noob>
                    while (true) {
                        MessageTree child = getReply(succ, 0);
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
                MessageTree pred = getSibling(inputBarTree, -1);
                if (pred == null) return false;
                moveInputBar(pred.getID());
                return true;
            case ROOT: // Just the root thread
                moveInputBar(null);
                return true;
        }
        return false;
    }

    public boolean tryEnsureVisible(@NonNull MessageTree mt, boolean expand) {
        return data.tryEnsureVisible(mt, expand);
    }

    public void toggleCollapse(@NonNull MessageTree mt, boolean newState) {
        data.setCollapsed(mt, newState);
    }

    public void toggleCollapse(@NonNull MessageTree mt) {
        data.toggleCollapsed(mt);
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        public final BaseMessageView itemMessageView;

        public ViewHolder(BaseMessageView v) {
            super(v);
            itemMessageView = v;
        }

    }

}
