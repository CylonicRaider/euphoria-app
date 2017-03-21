package io.euphoria.xkcd.app.control;

/** Created by Xyzzy on 2017-03-21. */

public class EventWrapper<T> {
    private final Class<T> cls;
    private final T event;

    public EventWrapper(Class<T> cls, T event) {
        this.cls = cls;
        this.event = event;
    }

    public Class<T> getEventClass() {
        return cls;
    }

    public T getEvent() {
        return event;
    }
}
