package storage.mongo

import config.DatabaseConfig
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo

class MongoDbSettings(private val config: DatabaseConfig){
    val mongoClient by lazy {
        KMongo.createClient(config.mongodb.connectionString).coroutine
    }

    val database by lazy {
        mongoClient.getDatabase(config.mongodb.databaseName)
    }

}