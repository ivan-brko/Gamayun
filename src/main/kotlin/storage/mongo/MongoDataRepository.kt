package storage.mongo

import org.bson.BsonDocument
import storage.DataRepository

class MongoDataRepository(val mongoDbSettings: MongoDbSettings): DataRepository {
    override suspend fun storeResult(jobId: String, result: List<BsonDocument>) {
        val collection = mongoDbSettings.database.getCollection<BsonDocument>(jobId)
        collection.insertMany(result)
    }

}