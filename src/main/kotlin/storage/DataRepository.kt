package storage

import processing.ProcessedGamayunResult

interface DataRepository {
    suspend fun storeResult(jobId: String, result: List<ProcessedGamayunResult>)
}