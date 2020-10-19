package storage.mongo

import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.InsertManyOptions
import config.ConfigurationReader
import config.JobDuplicateEntryPolicy
import config.OnDuplicateEntry
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.bson.BsonArray
import org.bson.BsonDocument
import org.bson.BsonString
import org.kodein.di.DI
import org.kodein.di.instance
import org.litote.kmongo.upsert
import processing.ProcessedGamayunResult
import storage.DataRepository

private val logger = KotlinLogging.logger {}

class MongoDataRepository(kodein: DI) : DataRepository {
    private val configurationReader: ConfigurationReader by kodein.instance()
    private val mongoDbSettings: MongoDbSettings by kodein.instance()

    init {
        runBlocking {
            val jobs = configurationReader.readJobsConfiguration()
            jobs.forEach { jobConfig ->
                if (jobConfig.jobDuplicateEntryPolicy?.uniqueIds != null) {
                    logger.info("Creating unique index for collection ${jobConfig.name}. Indices are: ${jobConfig.jobDuplicateEntryPolicy.uniqueIds}")
                    val collection = mongoDbSettings.database.getCollection<BsonDocument>(jobConfig.name)
                    collection.createIndex(Indexes.ascending(jobConfig.jobDuplicateEntryPolicy.uniqueIds), IndexOptions().unique(true))
                }
            }
        }
    }

    override suspend fun storeResult(jobId: String, result: List<ProcessedGamayunResult>, duplicateEntryPolicy: JobDuplicateEntryPolicy?) {
        if (duplicateEntryPolicy == null) {
            storeWithNoDuplicatePolicy(jobId, result)
        } else {
            when (duplicateEntryPolicy.onDuplicateEntryPolicy) {
                OnDuplicateEntry.IGNORE_NEW -> storeWithDuplicatePolicyIgnoreNew(jobId, result)
                OnDuplicateEntry.STORE_NEW -> storeWithDuplicatePolicyStoreNew(jobId, result, duplicateEntryPolicy.uniqueIds)
                OnDuplicateEntry.TRACK_CHANGES -> {
                }
            }
        }
    }

    private suspend fun storeWithDuplicatePolicyIgnoreNew(jobId: String, result: List<ProcessedGamayunResult>) {
        val collection = mongoDbSettings.database.getCollection<BsonDocument>(jobId)
        try {
            //if ordered is set to true (default value) entire write fails if write for a single document fails
            //failed writes for single documents can happen here because we might be trying to write a same document multiple times
            //(for example, if we parse the same ad in two different scheduler runs) if uniqueIndex is used
            //therefore, we don't want everything to fail if some documents fail, so we are setting ordered to false
            //see: https://docs.mongodb.com/php-library/master/reference/method/MongoDBCollection-insertMany/
            collection.insertMany(result.map { it.toBsonDocument() }, InsertManyOptions().ordered(false))
        } catch (e: com.mongodb.MongoBulkWriteException) { //todo: think about catching something more specific here or logging warning if it is not rewrite error
            //this exception happens when we are trying to insert a document whose unique ID is already present in MongoDB
            //this actually happens often in Gamayun scenario and it is not an error
            logger.debug(e.toString())
        }
    }

    private suspend fun storeWithDuplicatePolicyStoreNew(jobId: String, result: List<ProcessedGamayunResult>, uniqueIds: List<String>) {
        val collection = mongoDbSettings.database.getCollection<BsonDocument>(jobId)
        result.forEach { document ->
            val keysWithValues = uniqueIds
                    .map { uniqueId -> Pair(uniqueId, document.stringData[uniqueId]) }
                    .filter { it.second != null }
                    .toMap()

            val updateKeysMatcher = BsonDocument().also { document ->
                keysWithValues.forEach { keyWithValue ->
                    document[keyWithValue.key] = BsonString(keyWithValue.value)
                }
            }.toJson()

            collection.updateOne(updateKeysMatcher, document, upsert())
        }
    }

    //for mongo we can  treat these two situations as the same
    private suspend fun storeWithNoDuplicatePolicy(jobId: String, result: List<ProcessedGamayunResult>) =
            storeWithDuplicatePolicyIgnoreNew(jobId, result)

    private suspend fun storeWithDuplicatePolicyTrackChanges(jobId: String, result: List<ProcessedGamayunResult>, uniqueIds: List<String>) {
        TODO("This feature needs further thinking on how exactly we want the data stored in the DB")
    }


    private fun ProcessedGamayunResult.toBsonDocument(): BsonDocument =
            BsonDocument().also { document ->
                stringListData.forEach { keyValuePair ->
                    document[keyValuePair.key] = BsonArray().also { array ->
                        keyValuePair.value.forEach { value ->
                            array.add(BsonString(value))
                        }
                    }
                }

                stringData.forEach { keyValuePair ->
                    document[keyValuePair.key] = BsonString(keyValuePair.value)
                }
            }

}