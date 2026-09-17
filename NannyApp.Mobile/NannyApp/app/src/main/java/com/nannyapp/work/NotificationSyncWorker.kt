package com.nannyapp.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nannyapp.data.preferences.SessionManager
import com.nannyapp.domain.repository.NotificationRepository
import com.nannyapp.util.Resource
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Periodic background sync so the notification badge (request #22) stays fresh
 * even when the person isn't actively looking at the Notifications screen —
 * this is the "WorkManager where background work is required" piece from the
 * technology requirements. Runs only while a session exists; a no-op (success)
 * otherwise so it doesn't keep retrying after logout.
 */
@HiltWorker
class NotificationSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val notificationRepository: NotificationRepository,
    private val sessionManager: SessionManager,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val loggedIn = sessionManager.isLoggedInFlow.first()
        if (!loggedIn) return Result.success()

        return when (notificationRepository.refresh()) {
            is Resource.Success -> Result.success()
            is Resource.Error -> Result.retry()
            Resource.Loading -> Result.retry()
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "notification_sync"
    }
}
