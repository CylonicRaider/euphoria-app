package io.euphoria.xkcd.app.impl.ui;

import io.euphoria.xkcd.app.data.Message;

/** Created by Xyzzy on 2020-03-19. */

public class UIMessage {

    private final String id;
    private final String parent;
    private final long timestamp;
    private final String senderAgent;
    private final String senderName;
    private final String content;
    private final boolean truncated;

    public UIMessage(Message source) {
        this.id = source.getID();
        this.parent = source.getParent();
        this.timestamp = source.getTimestamp();
        this.senderAgent = source.getSender().getAgentID();
        this.senderName = source.getSender().getName();
        this.content = source.getContent();
        this.truncated = source.isTruncated();
    }

    public String getID() {
        return id;
    }

    public String getParent() {
        return parent;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getSenderAgent() {
        return senderAgent;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getContent() {
        return content;
    }

    public boolean isTruncated() {
        return truncated;
    }

}
