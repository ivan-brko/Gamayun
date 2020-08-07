package config

data class JobConfig(
    val name: String,
    val pathToExecutable: String,
    val args: List<String>,
    val cron: String,
    val resultWaitTimeoutMillis: Long,
    val tags: List<String>
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

data class ErrorReportConfig(
    val mailConfiguration: MailConfiguration?
)

interface ConfigurationReader {
    fun readJobsConfiguration(): List<JobConfig>
    fun readDatabaseConfiguration(): DatabaseConfig
    fun readErrorReportingConfiguration(): ErrorReportConfig
}