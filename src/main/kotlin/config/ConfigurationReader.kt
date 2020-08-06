package config

data class JobConfig(
    val name: String,
    val pathToExecutable: String,
    val args: List<String>,
    val cron: String,
    val resultWaitTimeoutMillis: Long
)

data class MongoDbConfig(
    val connectionString: String,
    val databaseName: String
)

data class DatabaseConfig(
    val mongodb: MongoDbConfig
)

interface ConfigurationReader {
    fun readJobsConfiguration(configurationRoot: String): List<JobConfig>
    fun readDatabaseConfiguration(configurationRoot: String): DatabaseConfig
}