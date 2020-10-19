package config

data class ApplicationConfiguration(
        val heartbeatPeriodInSeconds: Long?
)

enum class OnDuplicateEntry {
    IGNORE_NEW, STORE_NEW, TRACK_CHANGES
}

data class JobDuplicateEntryPolicy(
        val uniqueIds: List<String>,
        val onDuplicateEntryPolicy: OnDuplicateEntry
)

data class JobConfig(
        val name: String,
        val pathToExecutable: String,
        val args: List<String>?,
        val cron: String,
        val resultWaitTimeoutMillis: Long,
        val tags: List<String>?,
        val jobDuplicateEntryPolicy: JobDuplicateEntryPolicy?,
        val randomTriggerOffsetSeconds: Long?
)

data class MongoDbConfig(
        val connectionString: String,
        val databaseName: String
)

data class MailConfiguration(
        val recipientName: String,
        val recipientEmail: String,
        val senderName: String,
        val senderEmail: String,
        val smtpHost: String,
        val smtpPort: Long,
        val mailUsername: String,
        val mailPassword: String
)

data class DatabaseConfig(
        val mongodb: MongoDbConfig?
)

data class NotificationConfiguration(
        val mailConfiguration: MailConfiguration?
)

interface ConfigurationReader {
    fun readApplicationConfiguration(): ApplicationConfiguration
    fun readJobsConfiguration(): List<JobConfig>
    fun readDatabaseConfiguration(): DatabaseConfig
    fun readNotificationConfiguration(): NotificationConfiguration
}