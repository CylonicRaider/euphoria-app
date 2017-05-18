package io.euphoria.xkcd.app.impl.ui;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

import io.euphoria.xkcd.app.data.Message;
import io.euphoria.xkcd.app.data.SessionView;

/**
 * @author N00bySumairu
 */

class UIMessageWrapper implements Message {

    private Message msg;
    private List<MessageUpdateListener> updateListeners = new ArrayList<>();

    public static UIMessageWrapper wrap(@NonNull Message message) {
        if (message instanceof UIMessageWrapper) {
            return (UIMessageWrapper) message;
        } else {
            return new UIMessageWrapper(message);
        }
    }

    private UIMessageWrapper(Message msg) {
        this.msg = msg;
    }

    @Override
    public String getID() {
        return msg.getID();
    }

    @Override
    public String getParent() {
        return msg.getParent();
    }

    @Override
    public long getTimestamp() {
        return msg.getTimestamp();
    }

    @Override
    public SessionView getSender() {
        return msg.getSender();
    }

    @Override
    public String getContent() {
        return msg.getContent();
    }

    @Override
    public boolean isTruncated() {
        return msg.isTruncated();
    }

    public void update(Message updatedMsg) {
        msg = updatedMsg;
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
