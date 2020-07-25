package storage

class PrimitiveDataRepository : DataRepository {
    override fun storeResult(jobId: String, result: List<String>) {
        println("Storing result $result for jobId $jobId")
    }
}