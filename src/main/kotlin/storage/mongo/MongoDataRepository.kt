package storage.mongo

import org.bson.BsonDocument
import org.kodein.di.DI
import org.kodein.di.instance
import storage.DataRepository

class MongoDataRepository(kodein: DI) : DataRepository {
    private val mongoDbSettings: MongoDbSettings by kodein.instance()
    override suspend fun storeResult(jobId: String, result: List<BsonDocument>) {
        val collection = mongoDbSettings.database.getCollection<BsonDocument>(jobId)
        collection.insertMany(result)
    }

}