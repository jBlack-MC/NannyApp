package com.nannyapp.data.preferences

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Session transitions and cache access share one lock, including late network responses. */
class SessionCacheBoundary(private val currentToken: suspend () -> String?) {
    private val mutex = Mutex()

    suspend fun <T> transition(block: suspend () -> T): T = mutex.withLock { block() }

    suspend fun <T> access(token: String?, block: suspend () -> T): T? = mutex.withLock {
        if (token == null || token != currentToken()) null else block()
    }
}
