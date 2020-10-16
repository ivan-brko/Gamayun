package storage.mongo

import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.InsertManyOptions
import config.ConfigurationReader
import config.OnDuplicateEntry
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.bson.BsonDocument
import org.kodein.di.DI
import org.kodein.di.instance
import org.litote.kmongo.upsert
import storage.DataRepository

private val logger = KotlinLogging.logger {}

class MongoDataRepository(kodein: DI) : DataRepository {
    private val configurationReader: ConfigurationReader by kodein.instance()
    private val mongoDbSettings: MongoDbSettings by kodein.instance()
    private val jobs = configurationReader.readJobsConfiguration()

    init {
        runBlocking {
            jobs.forEach { jobConfig ->
                if (jobConfig.jobDuplicateEntryPolicy != null) {
                    logger.info("Creating unique index for collection ${jobConfig.name}. Indices are: ${jobConfig.jobDuplicateEntryPolicy.uniqueIds}")
                    val collection = mongoDbSettings.database.getCollection<BsonDocument>(jobConfig.name)
                    collection.createIndex(Indexes.ascending(jobConfig.jobDuplicateEntryPolicy.uniqueIds), IndexOptions().unique(true))
                }
            }
        }
    }

    override suspend fun storeResult(jobId: String, result: List<BsonDocument>) {
        val jobDuplicatePolicy = jobs.firstOrNull { it.name == jobId }?.jobDuplicateEntryPolicy
        if (jobDuplicatePolicy != null){
            when(jobDuplicatePolicy.onDuplicateEntry){
                OnDuplicateEntry.IGNORE_NEW -> storeResultWithDuplicatePolicyIgnoreNew(jobId, result)
                OnDuplicateEntry.STORE_NEW -> storeResultWithDuplicatePolicyStoreNew(jobId, result)
                OnDuplicateEntry.TRACK_CHANGES -> {}
            }
        } else {
            storeResultWithoutDuplicatePolicy(jobId, result)
        }
    }

    private suspend fun storeResultWithoutDuplicatePolicy(jobId: String, result: List<BsonDocument>) {
        val collection = mongoDbSettings.database.getCollection<BsonDocument>(jobId)
        collection.insertMany(result)
    }

    private suspend fun storeResultWithDuplicatePolicyIgnoreNew(jobId: String, result: List<BsonDocument>) {
        val collection = mongoDbSettings.database.getCollection<BsonDocument>(jobId)
        try {
            //if ordered is set to true (default value) entire write fails if write for a single document fails
            //failed writes for single documents can happen here because we might be trying to write a same document multiple times
            //(for example, if we parse the same ad in two different scheduler runs) if uniqueIndex is used
            //therefore, we don't want everything to fail if some documents fail, so we are setting ordered to false
            //see: https://docs.mongodb.com/php-library/master/reference/method/MongoDBCollection-insertMany/
            collection.insertMany(result, InsertManyOptions().ordered(false))
        } catch (e: com.mongodb.MongoBulkWriteException) {
            logger.debug(e.toString())
        }
    }

    private suspend fun storeResultWithDuplicatePolicyStoreNew(jobId: String, results: List<BsonDocument>) {
        val collection = mongoDbSettings.database.getCollection<BsonDocument>(jobId)
        //todo: this solution is not optimized at all!
        results.forEach { document ->
            val uniqueIds = getUniqueIdsForJobId(jobId)!!
            val keysWithValues = uniqueIds.map { uniqueId ->
                Pair(uniqueId, document[uniqueId]!!.asString())
            }.toMap()

            val updateKeysMatcher = BsonDocument().also { document ->
                keysWithValues.forEach { keyWithValue ->
                    document[keyWithValue.key] = keyWithValue.value
                }
            }.toJson()

            collection.updateOne(updateKeysMatcher, document, upsert())
        }
    }

    private fun getUniqueIdsForJobId(jobId: String): List<String>? =
            jobs.firstOrNull { it.name == jobId }?.let { it.jobDuplicateEntryPolicy?.uniqueIds }

}