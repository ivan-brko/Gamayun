package storage

import config.JobDuplicateEntryPolicy
import processing.ProcessedGamayunResult

interface DataRepository {
    suspend fun storeResult(
        jobId: String,
        result: List<ProcessedGamayunResult>,
        duplicateEntryPolicy: JobDuplicateEntryPolicy? = null
    )
}