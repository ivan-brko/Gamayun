package config

import com.moandjiezana.toml.Toml
import mu.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

class TomlConfigurationReader : ConfigurationReader {
    private fun File.parseTomlJobFile(): JobConfig? =
        Toml().read(readText()).let { parsedToml ->
            val neededTomlValues = listOf("name", "pathToExecutable", "cronString", "arguments", "resultWaitTimeoutMillis")

            return if (neededTomlValues.all { parsedToml.contains(it) }) {
                val name = parsedToml.getString("name")
                val exePath = parsedToml.getString("pathToExecutable")
                val cronString = parsedToml.getString("cronString")
                val arguments = parsedToml.getList<String>("arguments")
                val resultWaitTimeoutMillis = parsedToml.getLong("resultWaitTimeoutMillis")
                JobConfig(name, exePath, arguments, cronString, resultWaitTimeoutMillis)
            } else {
                logger.warn { "Configuration file $absoluteFile doesn't contain valid configuration. Will ignore file!" }
                null
            }
        }

    override fun readConfiguration(configurationRoot: String): List<JobConfig> {
        logger.info { "Reading configurations from configuration root $configurationRoot" }

        return File(configurationRoot)
            .walk()
            .filter { it.name.endsWith(".gamayunConf.toml") }
            .onEach { logger.debug { "Found a configuration file: ${it.absoluteFile}" } }
            .map { it.parseTomlJobFile() }
            .filterNotNull()
            .toList()
    }
}