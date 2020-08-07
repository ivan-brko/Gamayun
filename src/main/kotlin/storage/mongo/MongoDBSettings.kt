package storage.mongo

import config.DatabaseConfig
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo

class MongoDbSettings(private val config: DatabaseConfig) {
    private val mongoConfig by lazy {
        config.mongodb ?: throw IllegalArgumentException("Mongo config not present")
    }

    val mongoClient by lazy {
        KMongo.createClient(mongoConfig.connectionString).coroutine
    }

    val database by lazy {
        mongoClient.getDatabase(mongoConfig.databaseName)
    }

}