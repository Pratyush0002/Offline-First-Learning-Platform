package com.studenthelper.crdt.clock;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Version Vector representing the known causal history across all client devices.
 * Maps clientId -> highest observed Lamport logical clock.
 */
public class VersionVector implements Serializable, Cloneable {
    private static final long serialVersionUID = 1L;

    @JsonProperty("clocks")
    private final Map<String, Long> clocks;

    public VersionVector() {
        this.clocks = new ConcurrentHashMap<>();
    }

    @JsonCreator
    public VersionVector(@JsonProperty("clocks") Map<String, Long> clocks) {
        this.clocks = clocks != null ? new ConcurrentHashMap<>(clocks) : new ConcurrentHashMap<>();
    }

    public long getClock(String clientId) {
        return clocks.getOrDefault(clientId, 0L);
    }

    public void setClock(String clientId, long clock) {
        clocks.put(clientId, clock);
    }

    /**
     * Updates the clock for clientId to be max(existing, newClock).
     */
    public synchronized void updateMax(String clientId, long newClock) {
        clocks.merge(clientId, newClock, Math::max);
    }

    /**
     * Checks if this version vector contains or dominates the given operation.
     */
    public boolean hasSeen(String clientId, long clock) {
        return getClock(clientId) >= clock;
    }

    /**
     * Merges another version vector into this one: pointwise maximum.
     */
    public synchronized VersionVector merge(VersionVector other) {
        if (other == null) return this;
        for (Map.Entry<String, Long> entry : other.clocks.entrySet()) {
            this.updateMax(entry.getKey(), entry.getValue());
        }
        return this;
    }

    /**
     * Returns true if this vector strictly dominates or equals the other vector
     * for all known clients.
     */
    public boolean dominates(VersionVector other) {
        if (other == null) return true;
        for (Map.Entry<String, Long> entry : other.clocks.entrySet()) {
            if (this.getClock(entry.getKey()) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines causal relationship between two vectors:
     * -1: this happened before other
     *  1: this happened after other
     *  0: this equals other or they are concurrent (concurrently edited offline)
     */
    public int compareCausality(VersionVector other) {
        boolean greater = false;
        boolean smaller = false;

        Set<String> allClients = new HashSet<>(this.clocks.keySet());
        allClients.addAll(other.clocks.keySet());

        for (String client : allClients) {
            long c1 = this.getClock(client);
            long c2 = other.getClock(client);
            if (c1 > c2) greater = true;
            if (c1 < c2) smaller = true;
        }

        if (greater && !smaller) return 1;
        if (smaller && !greater) return -1;
        return 0; // Concurrent or Identical
    }

    public Map<String, Long> getClocks() {
        return Collections.unmodifiableMap(clocks);
    }

    @Override
    public VersionVector clone() {
        return new VersionVector(new HashMap<>(this.clocks));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VersionVector that)) return false;
        return Objects.equals(clocks, that.clocks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clocks);
    }

    @Override
    public String toString() {
        return "VersionVector" + clocks;
    }
}
