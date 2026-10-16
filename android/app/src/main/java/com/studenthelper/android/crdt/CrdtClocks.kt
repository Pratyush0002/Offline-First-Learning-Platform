package com.studenthelper.android.crdt

import java.io.Serializable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class LamportClock(initialValue: Long = 0) {
    private val counter = AtomicLong(initialValue)

    fun tick(): Long = counter.incrementAndGet()

    fun update(receivedTime: Long): Long {
        return counter.updateAndGet { current -> maxOf(current, receivedTime) + 1 }
    }

    fun get(): Long = counter.get()
    fun set(value: Long) = counter.set(value)
}

data class VersionVector(
    val clocks: MutableMap<String, Long> = ConcurrentHashMap()
) : Serializable {

    fun getClock(clientId: String): Long = clocks[clientId] ?: 0L

    fun updateMax(clientId: String, newClock: Long) {
        val current = getClock(clientId)
        clocks[clientId] = maxOf(current, newClock)
    }

    fun hasSeen(clientId: String, clock: Long): Boolean = getClock(clientId) >= clock

    fun merge(other: VersionVector): VersionVector {
        for ((client, clock) in other.clocks) {
            updateMax(client, clock)
        }
        return this
    }
}

data class FractionalIndex(
    val position: String = DEFAULT_START,
    val clientId: String = "default",
    val clock: Long = 0L
) : Comparable<FractionalIndex>, Serializable {

    override fun compareTo(other: FractionalIndex): Int {
        val posCmp = this.position.compareTo(other.position)
        if (posCmp != 0) return posCmp
        val clientCmp = this.clientId.compareTo(other.clientId)
        if (clientCmp != 0) return clientCmp
        return this.clock.compareTo(other.clock)
    }

    companion object {
        const val BASE_DIGITS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
        val MIN_CHAR = BASE_DIGITS.first()
        val MAX_CHAR = BASE_DIGITS.last()
        const val DEFAULT_START = "a0"

        fun initial(clientId: String, clock: Long): FractionalIndex {
            return FractionalIndex(DEFAULT_START, clientId, clock)
        }

        fun between(prev: FractionalIndex?, next: FractionalIndex?, clientId: String, clock: Long): FractionalIndex {
            val prevPos = prev?.position ?: ""
            val nextPos = next?.position ?: ""
            val newPos = generateKeyBetween(prevPos, nextPos)
            return FractionalIndex(newPos, clientId, clock)
        }

        private fun generateKeyBetween(prev: String, next: String): String {
            if (prev.isEmpty() && next.isEmpty()) return DEFAULT_START
            if (prev.isEmpty()) return decrementKey(next)
            if (next.isEmpty()) return incrementKey(prev)

            val maxLen = maxOf(prev.length, next.length)
            val builder = StringBuilder()

            for (i in 0..maxLen + 1) {
                val p = if (i < prev.length) prev[i] else MIN_CHAR
                val n = if (i < next.length) next[i] else MAX_CHAR

                if (p == n) {
                    builder.append(p)
                } else if (n - p > 1) {
                    val mid = (p.code + (n - p) / 2).toChar()
                    builder.append(mid)
                    return builder.toString()
                } else {
                    builder.append(p)
                    val nextP = if (i + 1 < prev.length) prev[i + 1] else MIN_CHAR
                    var mid2 = (nextP.code + (MAX_CHAR - nextP) / 2).toChar()
                    if (mid2 <= nextP) mid2 = (nextP.code + 1).toChar()
                    builder.append(mid2)
                    return builder.toString()
                }
            }
            builder.append('m')
            return builder.toString()
        }

        private fun incrementKey(key: String): String {
            val last = key.last()
            return if (last < MAX_CHAR) {
                key.substring(0, key.length - 1) + (last.code + 1).toChar()
            } else {
                key + "V"
            }
        }

        private fun decrementKey(key: String): String {
            val first = key.first()
            return if (first > '1') {
                (first.code - 1).toChar() + key.substring(1)
            } else {
                "0$key"
            }
        }
    }
}
