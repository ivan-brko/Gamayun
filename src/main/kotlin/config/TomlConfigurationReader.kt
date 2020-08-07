package config

import com.moandjiezana.toml.Toml
import mu.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

class TomlConfigurationReader(val configurationRoot: String) : ConfigurationReader {
    private fun File.parseTomlJobFile(): JobConfig? =
        Toml().read(readText()).let { parsedToml ->
            val neededTomlValues =
                listOf("name", "pathToExecutable", "cronString", "arguments", "resultWaitTimeoutMillis", "tags")

            return if (neededTomlValues.all { parsedToml.contains(it) }) {
                val name = parsedToml.getString("name")
                val exePath = parsedToml.getString("pathToExecutable")
                val cronString = parsedToml.getString("cronString")
                val arguments = parsedToml.getList<String>("arguments")
                val resultWaitTimeoutMillis = parsedToml.getLong("resultWaitTimeoutMillis")
                val tags = parsedToml.getList<String>("tags")
                JobConfig(name, exePath, arguments, cronString, resultWaitTimeoutMillis, tags)
            } else {
                logger.warn { "Configuration file $absoluteFile doesn't contain valid configuration. Will ignore file!" }
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

    private fun File.parseEmailFile(): ErrorReportConfig? =
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

                ErrorReportConfig(
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
            ?.let { it.parseTomlDatabaseFile() } ?: throw IllegalArgumentException("No valid database configuration!")
    }

    override fun readErrorReportingConfiguration(): ErrorReportConfig {
        logger.info { "Reading email configuration from configuration root $configurationRoot" }

        return File(configurationRoot)
            .walk()
            .filter { it.name == "gamayun-email.config.toml" }
            .firstOrNull()
            ?.also { logger.debug { "Found an email configuration file: ${it.absoluteFile}" } }
            ?.let { it.parseEmailFile() } ?: throw IllegalArgumentException("No valid email configuration!")
    }
}