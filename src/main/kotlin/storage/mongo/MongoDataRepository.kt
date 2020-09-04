package storage.mongo

import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.InsertManyOptions
import config.ConfigurationReader
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.bson.BsonDocument
import org.kodein.di.DI
import org.kodein.di.instance
import storage.DataRepository

private val logger = KotlinLogging.logger {}

class MongoDataRepository(kodein: DI) : DataRepository {
    private val configurationReader: ConfigurationReader by kodein.instance()
    private val mongoDbSettings: MongoDbSettings by kodein.instance()

    init {
        runBlocking {
            val jobs = configurationReader.readJobsConfiguration()
            jobs.forEach { jobConfig ->
                if (jobConfig.uniqueIds != null) {
                    logger.info("Creating unique index for collection ${jobConfig.name}. Indices are: ${jobConfig.uniqueIds}")
                    val collection = mongoDbSettings.database.getCollection<BsonDocument>(jobConfig.name)
                    collection.createIndex(Indexes.ascending(jobConfig.uniqueIds), IndexOptions().unique(true))
                }
            }
        }
    }

    override suspend fun storeResult(jobId: String, result: List<BsonDocument>) {
        val collection = mongoDbSettings.database.getCollection<BsonDocument>(jobId)
        try {
            //if ordered is set to true (default value) entire write fails if write for a single document fails
            //failed writes for single documents can happen here because we might be trying to write a same document multiple times
            //(for example, if we parse the same ad in two different scheduler runs) if uniqueIndex is used
            //therefore, we don't want everything to fail if some documents fail, so we are setting ordered to false
            //see: https://docs.mongodb.com/php-library/master/reference/method/MongoDBCollection-insertMany/
            collection.insertMany(result, InsertManyOptions().ordered(false))
        } catch (e: com.mongodb.MongoBulkWriteException) {
            logger.warn(e.toString())
        }
    }

}