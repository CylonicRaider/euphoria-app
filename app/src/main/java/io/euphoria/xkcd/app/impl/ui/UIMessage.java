package io.euphoria.xkcd.app.impl.ui;

import java.util.Locale;

import io.euphoria.xkcd.app.data.Message;

/** Created by Xyzzy on 2020-03-19. */

public class UIMessage {

    private static boolean equalsOrNull(Object a, Object b) {
        return (a == null) ? (b == null) : a.equals(b);
    }

    private static int hashCodeOrNull(Object a) {
        return (a == null) ? 0 : a.hashCode();
    }

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
        return hashCodeOrNull(id) ^ hashCodeOrNull(parent) << 4 ^ longHashCode(timestamp) ^
                hashCodeOrNull(senderAgent) << 4 ^ hashCodeOrNull(senderName) << 8 ^ hashCodeOrNull(content) ^
                booleanHashCode(truncated);
    }

    public boolean equals(Object other) {
        if (!(other instanceof UIMessage)) return false;
        UIMessage mo = (UIMessage) other;
        return (equalsOrNull(getID(), mo.getID()) && equalsOrNull(getParent(), mo.getParent()) &&
                getTimestamp() == mo.getTimestamp() && equalsOrNull(getSenderAgent(), mo.getSenderAgent()) &&
                equalsOrNull(getSenderName(), mo.getSenderName()) && equalsOrNull(getContent(), mo.getContent()) &&
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
