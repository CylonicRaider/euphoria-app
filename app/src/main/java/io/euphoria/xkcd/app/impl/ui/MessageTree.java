package io.euphoria.xkcd.app.impl.ui;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.euphoria.xkcd.app.data.Message;
import io.euphoria.xkcd.app.data.SessionView;

/**
 * @author N00bySumairu
 */

public class MessageTree implements Message {

    public static MessageTree wrap(@NonNull Message m) {
        return (m instanceof MessageTree) ? (MessageTree) m : new MessageTree(m);
    }

    private Message message;
    private List<MessageTree> replies = new ArrayList<>();
    private int indent = 0;
    private boolean collapsed = false;

    private MessageTree(@NonNull Message m) {
        message = m;
    }

    @Override
    public String getID() {
        return message.getID();
    }

    @Override
    public String getParent() {
        return message.getParent();
    }

    @Override
    public long getTimestamp() {
        return message.getTimestamp();
    }

    @Override
    public SessionView getSender() {
        return message.getSender();
    }

    @Override
    public String getContent() {
        return message.getContent();
    }

    @Override
    public boolean isTruncated() {
        return message.isTruncated();
    }

    public int getIndent() {
        return indent;
    }

    public void addReply(@NonNull Message m) {
        MessageTree mt = MessageTree.wrap(m);
        mt.indent = indent + 1;
        replies.add(mt);
    }

    public List<MessageTree> getReplies() {
        return Collections.unmodifiableList(replies);
    }

    public void updateMessage(@NonNull Message m) {
        message = MessageTree.wrap(m);
    }

    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
    }

    public boolean isCollapsed() {
        return collapsed;
    }
}
