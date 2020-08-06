package storage

import org.bson.BsonDocument

interface DataRepository {
    suspend fun storeResult(jobId: String, result: List<BsonDocument>)
}