package io.euphoria.xkcd.app.control;

/** Created by Xyzzy on 2017-03-25. */

public class PausingEventQueue<T> extends EventQueue<T> {
    private boolean paused;

    public PausingEventQueue(Runnable schedule) {
        super(schedule);
    }

    public synchronized boolean isPaused() {
        return paused;
    }

    public synchronized void setPaused(boolean paused) {
        this.paused = paused;
        if (paused) clear();
    }

    @Override
    public synchronized void add(T e) {
        if (! paused) super.add(e);
    }
}
