package com.studenthelper.crdt.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Deterministic fractional indexing for CRDT block ordering.
 * Generates lexicographically orderable position keys between any two arbitrary positions,
 * incorporating client identifiers to break ties deterministically during concurrent offline insertions.
 */
public class FractionalIndex implements Comparable<FractionalIndex>, Serializable {
    private static final long serialVersionUID = 1L;

    public static final String BASE_DIGITS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final char MIN_CHAR = BASE_DIGITS.charAt(0);
    private static final char MAX_CHAR = BASE_DIGITS.charAt(BASE_DIGITS.length() - 1);
    private static final String DEFAULT_START = "a0";

    private final String position;
    private final String clientId;
    private final long clock;

    public FractionalIndex(String position, String clientId, long clock) {
        this.position = position != null ? position : DEFAULT_START;
        this.clientId = clientId != null ? clientId : "default";
        this.clock = clock;
    }

    public static FractionalIndex initial(String clientId, long clock) {
        return new FractionalIndex(DEFAULT_START, clientId, clock);
    }

    /**
     * Generates a new FractionalIndex strictly between prev and next.
     * Either prev or next may be null (representing start or end of list).
     */
    public static FractionalIndex between(FractionalIndex prev, FractionalIndex next, String clientId, long clock) {
        String prevPos = prev != null ? prev.position : "";
        String nextPos = next != null ? next.position : "";

        String newPos = generateKeyBetween(prevPos, nextPos);
        return new FractionalIndex(newPos, clientId, clock);
    }

    private static String generateKeyBetween(String prev, String next) {
        if (prev.isEmpty() && next.isEmpty()) {
            return DEFAULT_START;
        }
        if (prev.isEmpty()) {
            // Insert before next
            return decrementKey(next);
        }
        if (next.isEmpty()) {
            // Insert after prev
            return incrementKey(prev);
        }

        // Insert strictly between prev and next
        int len = Math.max(prev.length(), next.length());
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i <= len + 1; i++) {
            char p = (i < prev.length()) ? prev.charAt(i) : MIN_CHAR;
            char n = (i < next.length()) ? next.charAt(i) : MAX_CHAR;

            if (p == n) {
                builder.append(p);
            } else if (n - p > 1) {
                // There is room between characters
                char mid = (char) (p + (n - p) / 2);
                builder.append(mid);
                return builder.toString();
            } else {
                // Adjacent characters, e.g. 'a' and 'b'. Keep 'a' and go to next digit
                builder.append(p);
                // Append midpoint between next character and MAX_CHAR
                char nextP = (i + 1 < prev.length()) ? prev.charAt(i + 1) : MIN_CHAR;
                char mid2 = (char) (nextP + (MAX_CHAR - nextP) / 2);
                if (mid2 <= nextP) mid2 = (char) (nextP + 1);
                builder.append(mid2);
                return builder.toString();
            }
        }

        builder.append('m');
        return builder.toString();
    }

    private static String incrementKey(String key) {
        char last = key.charAt(key.length() - 1);
        if (last < MAX_CHAR) {
            return key.substring(0, key.length() - 1) + (char) (last + 1);
        }
        return key + "V";
    }

    private static String decrementKey(String key) {
        char first = key.charAt(0);
        if (first > '0' + 1) {
            return (char) (first - 1) + key.substring(1);
        }
        return "0" + key;
    }

    public String getPosition() {
        return position;
    }

    public String getClientId() {
        return clientId;
    }

    public long getClock() {
        return clock;
    }

    @Override
    public int compareTo(FractionalIndex o) {
        if (o == null) return 1;
        int cmp = this.position.compareTo(o.position);
        if (cmp != 0) return cmp;
        // Deterministic tie-breaker using clientId and Lamport clock
        int clientCmp = this.clientId.compareTo(o.clientId);
        if (clientCmp != 0) return clientCmp;
        return Long.compare(this.clock, o.clock);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FractionalIndex that)) return false;
        return clock == that.clock &&
                Objects.equals(position, that.position) &&
                Objects.equals(clientId, that.clientId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(position, clientId, clock);
    }

    @Override
    public String toString() {
        return position + "[" + clientId + "@" + clock + "]";
    }
}
