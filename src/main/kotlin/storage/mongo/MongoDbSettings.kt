package storage.mongo

import config.ConfigurationReader
import org.kodein.di.DI
import org.kodein.di.instance
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo

class MongoDbSettings(kodein: DI) {
    private val configurationReader: ConfigurationReader by kodein.instance()

    private val mongoConfig by lazy {
        configurationReader.readDatabaseConfiguration().mongodb
                ?: throw IllegalArgumentException("Mongo config not present")
    }

    val mongoClient by lazy {
        KMongo.createClient(mongoConfig.connectionString).coroutine
    }

    val database by lazy {
        mongoClient.getDatabase(mongoConfig.databaseName)
    }

}