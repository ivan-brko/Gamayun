package storage

import org.bson.BsonDocument

class PrimitiveDataRepository : DataRepository {
    override suspend fun storeResult(jobId: String, result: List<BsonDocument>) {
        println("Storing result $result for jobId $jobId")
    }
}