package com.nannyapp

import com.nannyapp.data.preferences.SessionCacheBoundary
import com.nannyapp.util.Resource
import com.nannyapp.util.canUseOfflineCache
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class SessionCacheBoundaryTest {
    @Test fun accountSwitchRejectsLateResponseAndClearsData() = runBlocking {
        var token: String? = "synthetic-A"
        val rows = mutableListOf("A-child", "A-payment")
        val boundary = SessionCacheBoundary { token }
        boundary.transition { token = "synthetic-B"; rows.clear() }
        assertNull(boundary.access("synthetic-A") { rows.add("late-A") })
        assertEquals(emptyList<String>(), boundary.access("synthetic-B") { rows.toList() })
        boundary.access("synthetic-B") { rows.add("B-payment") }
        assertEquals(listOf("B-payment"), rows)
    }

    @Test fun logoutRejectsCachedAndInFlightData() = runBlocking {
        var token: String? = "synthetic-A"
        val boundary = SessionCacheBoundary { token }
        boundary.transition { token = null }
        assertNull(boundary.access("synthetic-A") { "private" })
        assertNull(boundary.access(null) { "private" })
    }

    @Test fun transitionWaitsForCacheWriteThenPurgesIt() = runBlocking {
        var token: String? = "synthetic-A"
        val rows = mutableListOf<String>()
        val boundary = SessionCacheBoundary { token }
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val write = launch { boundary.access("synthetic-A") { started.complete(Unit); release.await(); rows.add("A") } }
        started.await()
        val logout = launch { boundary.transition { token = null; rows.clear() } }
        yield()
        release.complete(Unit)
        joinAll(write, logout)
        assertTrue(rows.isEmpty())
    }

    @Test fun onlyTransportFailuresUseOfflineCache() {
        assertTrue(Resource.Error("offline", IOException()).canUseOfflineCache())
        for (code in listOf(401, 403, 429, 500)) {
            assertFalse(Resource.Error("HTTP", IOException(), code).canUseOfflineCache())
        }
        assertFalse(Resource.Error("invalid payload", IllegalStateException()).canUseOfflineCache())
    }
}
