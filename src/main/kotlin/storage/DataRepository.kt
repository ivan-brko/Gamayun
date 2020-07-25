package storage

interface DataRepository {
    fun storeResult(jobId: String, result: List<String>): Unit
}