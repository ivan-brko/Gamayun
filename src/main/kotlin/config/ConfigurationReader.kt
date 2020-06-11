package config

data class JobConfig(
    val name: String,
    val pathToExecutable: String,
    val args: List<String>,
    val cron: String
)

interface ConfigurationReader {
    fun readConfiguration(configurationRoot: String): List<JobConfig>
}