package io.euphoria.xkcd.app.control;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Created by Xyzzy on 2017-03-20. */

public class EventQueue<T> {
    private final ConcurrentLinkedQueue<T> queue;
    private final Runnable schedule;

    public EventQueue(Runnable schedule) {
        this.queue = new ConcurrentLinkedQueue<>();
        this.schedule = schedule;
    }
    public EventQueue() {
        this(null);
    }

    public synchronized boolean isEmpty() {
        return this.queue.isEmpty();
    }

    public synchronized void clear() {
        this.queue.clear();
    }

    public synchronized T get() {
        return this.queue.poll();
    }

    public synchronized List<T> getAll() {
        List<T> ret = new ArrayList<>(this.queue);
        this.queue.clear();
        return ret;
    }

    public synchronized void add(T e) {
        if (e == null) return;
        boolean doSched = this.queue.isEmpty();
        this.queue.add(e);
        if (doSched && this.schedule != null) this.schedule.run();
    }
}
