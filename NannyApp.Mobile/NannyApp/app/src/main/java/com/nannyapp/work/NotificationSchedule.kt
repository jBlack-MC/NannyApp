package com.nannyapp.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

private const val WORK_NAME = "notification-sync"

/** Starts or stops the background check for new in-app notifications (Settings → Notifications). */
fun scheduleNotificationSync(context: Context, enabled: Boolean) {
    val workManager = WorkManager.getInstance(context)
    if (!enabled) {
        workManager.cancelUniqueWork(WORK_NAME)
        return
    }
    val request = PeriodicWorkRequestBuilder<NotificationSyncWorker>(30, TimeUnit.MINUTES)
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .build()
    workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
}
