package storage

interface DataRepository {
    suspend fun storeResult(jobId: String, result: List<String>)
}