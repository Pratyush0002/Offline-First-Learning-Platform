package com.studenthelper.crdt.clock;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Lamport Logical Clock for establishing a total causal ordering of events
 * in an offline-first distributed system.
 */
public class LamportClock {
    private final AtomicLong counter;

    public LamportClock() {
        this(0);
    }

    public LamportClock(long initialValue) {
        this.counter = new AtomicLong(initialValue);
    }

    /**
     * Increments the clock for a new local event and returns the new value.
     */
    public long tick() {
        return counter.incrementAndGet();
    }

    /**
     * Updates the clock based on a timestamp received from a remote node:
     * clock = max(localClock, receivedTime) + 1
     */
    public long update(long receivedTime) {
        return counter.updateAndGet(current -> Math.max(current, receivedTime) + 1);
    }

    public long get() {
        return counter.get();
    }

    public void set(long value) {
        counter.set(value);
    }

    @Override
    public String toString() {
        return "LamportClock(" + counter.get() + ")";
    }
}
