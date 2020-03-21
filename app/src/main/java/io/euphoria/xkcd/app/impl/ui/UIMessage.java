package io.euphoria.xkcd.app.impl.ui;

import java.util.Locale;

import io.euphoria.xkcd.app.data.Message;

/** Created by Xyzzy on 2020-03-19. */

public class UIMessage {

    private static int longHashCode(long n) {
        return (int) (n >>> 32) ^ (int) n;
    }

    private static int booleanHashCode(boolean b) {
        return (b) ? 1231 : 1237;
    }

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

    public int hashCode() {
        return UIUtils.hashCodeOrNull(id) ^ UIUtils.hashCodeOrNull(parent) << 4 ^ longHashCode(timestamp) ^
                UIUtils.hashCodeOrNull(senderAgent) << 4 ^ UIUtils.hashCodeOrNull(senderName) << 8 ^
                UIUtils.hashCodeOrNull(content) ^ booleanHashCode(truncated);
    }

    public boolean equals(Object other) {
        if (!(other instanceof UIMessage)) return false;
        UIMessage mo = (UIMessage) other;
        return (UIUtils.equalsOrNull(getID(), mo.getID()) &&
                UIUtils.equalsOrNull(getParent(), mo.getParent()) &&
                getTimestamp() == mo.getTimestamp() &&
                UIUtils.equalsOrNull(getSenderAgent(), mo.getSenderAgent()) &&
                UIUtils.equalsOrNull(getSenderName(), mo.getSenderName()) &&
                UIUtils.equalsOrNull(getContent(), mo.getContent()) &&
                isTruncated() == mo.isTruncated());
    }

    public String toString() {
        return String.format((Locale) null,
                "%s@%h[id=%s,parent=%s,timestamp=%s,sender=[agent=%s,name=%s],content=%s,truncated=%s]",
                getClass().getSimpleName(), this, id, parent, timestamp, senderAgent, senderName, content, truncated);
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
