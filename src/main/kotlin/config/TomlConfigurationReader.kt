package config

import com.moandjiezana.toml.Toml
import mu.KotlinLogging
import java.io.File
import java.nio.file.Paths

private val logger = KotlinLogging.logger {}

class TomlConfigurationReader(private val configurationRoot: String) : ConfigurationReader {

    private fun String.replaceConfigurationFilePathPlaceholder(configurationPath: String): String =
        this.replace("\${CONFIGURATION_FILE_PATH}", configurationPath)

    private fun File.parseTomlJobFile(): JobConfig? =
        Toml().read(readText()).let { parsedToml ->
            val pathToConfig = Paths.get(absolutePath).parent.toString()
            val neededTomlValues =
                listOf("name", "pathToExecutable", "cronString")

            return if (neededTomlValues.all { parsedToml.contains(it) }) {
                val name = parsedToml.getString("name").replaceConfigurationFilePathPlaceholder(pathToConfig)
                val exePath =
                    parsedToml.getString("pathToExecutable").replaceConfigurationFilePathPlaceholder(pathToConfig)
                val cronString = parsedToml.getString("cronString")
                val arguments = parsedToml.getList<String>("arguments")
                    ?.map { it.replaceConfigurationFilePathPlaceholder(pathToConfig) }
                val resultWaitTimeoutMillis = parsedToml.getLong("resultWaitTimeoutMillis") ?: 5000
                val producesResult = parsedToml.getBoolean("producesResult") ?: true
                val randomTriggerOffsetSeconds = parsedToml.getLong("randomTriggerOffsetSeconds")
                val tags = parsedToml.getList<String>("tags")
                val duplicateEntryPolicy = parsedToml.getDuplicateEntryPolicy()

                JobConfig(
                    name,
                    exePath,
                    arguments,
                    cronString,
                    resultWaitTimeoutMillis,
                    tags,
                    duplicateEntryPolicy,
                    randomTriggerOffsetSeconds,
                    producesResult
                )
            } else {
                logger.warn { "Configuration file $absoluteFile doesn't contain valid configuration. Will ignore file!" }
                null
            }
        }

    private fun Toml.getDuplicateEntryPolicy(): JobDuplicateEntryPolicy? =
        getTable("duplicateEntryPolicy")?.let { table ->
            val uniqueIds: List<String>? = table.getList<String>("uniqueIds")
            val onDuplicateEntry: OnDuplicateEntry? = table.getString("onDuplicateEntry")?.let { onDupEntryString ->
                when (onDupEntryString) {
                    "IGNORE_NEW" -> OnDuplicateEntry.IGNORE_NEW
                    "STORE_NEW" -> OnDuplicateEntry.STORE_NEW
                    "TRACK_CHANGES" -> OnDuplicateEntry.TRACK_CHANGES
                    else -> null
                }
            }
            if (uniqueIds != null && onDuplicateEntry != null) {
                JobDuplicateEntryPolicy(uniqueIds, onDuplicateEntry)
            } else {
                null
            }
        }

    private fun File.parseTomlDatabaseFile(): DatabaseConfig? =
        Toml().read(readText()).let { parsedToml ->
            val neededTomlValues = listOf("connectionString", "databaseName")

            return if (neededTomlValues.all { parsedToml.contains(it) }) {
                val connectionString = parsedToml.getString("connectionString")
                val databaseName = parsedToml.getString("databaseName")
                DatabaseConfig(MongoDbConfig(connectionString, databaseName))
            } else {
                logger.warn { "Configuration file $absoluteFile doesn't contain valid configuration. Will ignore file!" }
                null
            }
        }

    private fun File.parseEmailFile(): NotificationConfiguration? =
        Toml().read(readText()).let { parsedToml ->
            val neededTomlValues = listOf(
                "recipientName",
                "recipientEmail",
                "senderEmail",
                "senderName",
                "smtpHost",
                "smtpPort",
                "mailUsername",
                "mailPassword"
            )

            return if (neededTomlValues.all { parsedToml.contains(it) }) {
                val recipientName = parsedToml.getString("recipientName")
                val recipientEmail = parsedToml.getString("recipientEmail")
                val senderName = parsedToml.getString("senderName")
                val senderEmail = parsedToml.getString("senderEmail")
                val smtpHost = parsedToml.getString("smtpHost")
                val smtpPort = parsedToml.getLong("smtpPort")
                val mailUsername = parsedToml.getString("mailUsername")
                val mailPassword = parsedToml.getString("mailPassword")

                NotificationConfiguration(
                    MailConfiguration(
                        recipientName,
                        recipientEmail,
                        senderName,
                        senderEmail,
                        smtpHost,
                        smtpPort,
                        mailUsername,
                        mailPassword
                    )
                )
            } else {
                logger.warn { "Configuration file $absoluteFile doesn't contain valid configuration. Will ignore file!" }
                null
            }
        }

    private fun File.readApplicationConfigurationFile(): ApplicationConfiguration =
        Toml().read(readText()).let { parsedToml ->
            val heartbeatPeriodInSeconds: Long? = parsedToml.getLong("heartbeatPeriodInSeconds")
            ApplicationConfiguration(heartbeatPeriodInSeconds = heartbeatPeriodInSeconds)
        }

    override fun readApplicationConfiguration(): ApplicationConfiguration {
        logger.info { "Reading application configuration from configuration root $configurationRoot" }

        return File(configurationRoot)
            .walk()
            .filter { it.name == "gamayun-application.config.toml" }
            .firstOrNull()
            ?.also { logger.debug { "Found an application configuration file: ${it.absoluteFile}" } }
            ?.let { it.readApplicationConfigurationFile() }
            ?: ApplicationConfiguration(heartbeatPeriodInSeconds = null)
    }

    override fun readJobsConfiguration(): List<JobConfig> {
        logger.info { "Reading job configurations from configuration root $configurationRoot" }

        return File(configurationRoot)
            .walk()
            .filter { it.name.endsWith(".gamayun-job.config.toml") }
            .onEach { logger.debug { "Found a job configuration file: ${it.absoluteFile}" } }
            .map { it.parseTomlJobFile() }
            .filterNotNull()
            .toList()
    }

    override fun readDatabaseConfiguration(): DatabaseConfig {
        logger.info { "Reading mongoDB configuration from configuration root $configurationRoot" }

        return File(configurationRoot)
            .walk()
            .filter { it.name == "gamayun-mongo.config.toml" }
            .firstOrNull()
            ?.also { logger.debug { "Found a mongo configuration file: ${it.absoluteFile}" } }
            ?.let { it.parseTomlDatabaseFile() }
            ?: throw IllegalArgumentException("No valid database configuration!")
    }

    override fun readNotificationConfiguration(): NotificationConfiguration {
        logger.info { "Reading email configuration from configuration root $configurationRoot" }

        return File(configurationRoot)
            .walk()
            .filter { it.name == "gamayun-email.config.toml" }
            .firstOrNull()
            ?.also { logger.debug { "Found an email configuration file: ${it.absoluteFile}" } }
            ?.let { it.parseEmailFile() } ?: NotificationConfiguration(mailConfiguration = null)
    }
}