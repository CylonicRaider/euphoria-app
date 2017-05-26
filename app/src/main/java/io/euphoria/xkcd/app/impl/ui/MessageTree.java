package io.euphoria.xkcd.app.impl.ui;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.euphoria.xkcd.app.data.Message;
import io.euphoria.xkcd.app.data.SessionView;

/**
 * @author N00bySumairu
 */

public class MessageTree implements Message {
    Message message;
    List<MessageTree> subTrees = new ArrayList<>();
    private List<MessageUpdateListener> updateListeners = new ArrayList<>();

    public static MessageTree wrap(@NonNull Message message) {
        if (message instanceof MessageTree) {
            return (MessageTree) message;
        } else {
            return new MessageTree(message);
        }
    }

    public MessageTree(Message message) {
        this.message = message;
    }

    public MessageTree(Message message, MessageTree... children) {
        this.message = message;
        subTrees.addAll(Arrays.asList(children));
    }

    public void addSubTree(MessageTree child) {
        subTrees.add(child);
    }

    public Message getMessage() {
        return message;
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

    public void updateMessage(Message updatedMsg) {
        message = updatedMsg;
        for (MessageUpdateListener updateListener : updateListeners) {
            updateListener.onUpdate();
        }
    }

    public void addUpdateListener(MessageUpdateListener updateListener) {
        updateListeners.add(updateListener);
    }

    public void removeUpdateListener(MessageUpdateListener updateListener) {
        updateListeners.remove(updateListener);
    }
}
