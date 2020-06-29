package storage

interface DataRepository {
    fun storeResult(jobId: String, result: String): Unit
}