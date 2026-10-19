package com.nannyapp.util

import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

/**
 * Unit tests for NetworkResult and the canUseOfflineCache extension on Resource.Error.
 * These run on the JVM with no Android dependencies.
 */
class OfflineCacheTest {

    @Test
    fun `transport IOException allows offline cache`() {
        val err = Resource.Error("Network unreachable", IOException())
        assertTrue("IOException should allow offline fallback", err.canUseOfflineCache())
    }

    @Test
    fun `HTTP 401 rejects offline cache`() {
        val err = Resource.Error("Unauthorised", IOException(), code = 401)
        assertFalse("401 must not use stale data", err.canUseOfflineCache())
    }

    @Test
    fun `HTTP 403 rejects offline cache`() {
        val err = Resource.Error("Forbidden", IOException(), code = 403)
        assertFalse(err.canUseOfflineCache())
    }

    @Test
    fun `HTTP 429 rejects offline cache`() {
        val err = Resource.Error("Rate limited", IOException(), code = 429)
        assertFalse(err.canUseOfflineCache())
    }

    @Test
    fun `HTTP 500 rejects offline cache`() {
        val err = Resource.Error("Server error", IOException(), code = 500)
        assertFalse(err.canUseOfflineCache())
    }

    @Test
    fun `non-IO exception rejects offline cache`() {
        val err = Resource.Error("JSON parse failure", IllegalStateException("bad json"))
        assertFalse("Parse errors should not silently use stale data", err.canUseOfflineCache())
    }

    @Test
    fun `null exception rejects offline cache`() {
        val err = Resource.Error("Unknown error", null)
        assertFalse(err.canUseOfflineCache())
    }
}
