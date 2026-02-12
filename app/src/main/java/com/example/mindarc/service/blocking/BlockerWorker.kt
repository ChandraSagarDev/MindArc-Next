package com.example.mindarc.service.blocking

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class BlockerWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val blockCache: BlockCache
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            blockCache.rebuildCache()
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
