package com.eirspay.data.fake

import kotlinx.coroutines.delay

/**
 * Simulated network latency for the in-memory repositories, matching the prototype loader (1.8s).
 * Set [millis] to 0 in tests.
 */
object FakeLatency {
    @Volatile var millis: Long = 1800
    suspend fun wait() { if (millis > 0) delay(millis) }
}

/** Deterministic pseudo-IRN (64 hex chars). The real IRN comes back from MoR on registration. */
object FakeIrn {
    fun of(seed: String): String {
        var s = 0L
        for (c in seed) s = (s * 31 + c.code) % 233280
        val sb = StringBuilder()
        repeat(64) {
            s = (s * 9301 + 49297) % 233280
            sb.append("0123456789abcdef"[(s * 16 / 233280).toInt()])
        }
        return sb.toString()
    }
}
