package io.euphoria.xkcd.app.control;

import java.util.concurrent.ConcurrentLinkedQueue;

/** Created by Xyzzy on 2017-03-20. */

public class EventQueue<T> {
    private final ConcurrentLinkedQueue<T> queue;
    private final Runnable schedule;

    public EventQueue(Runnable schedule) {
        this.queue = new ConcurrentLinkedQueue<>();
        this.schedule = schedule;
    }

    public synchronized boolean isEmpty() {
        return this.queue.isEmpty();
    }

    public synchronized T get() {
        return this.queue.poll();
    }

    public synchronized void put(T e) {
        if (e == null) return;
        boolean doSched = this.queue.isEmpty();
        this.queue.add(e);
        if (doSched) this.schedule.run();
    }
}
