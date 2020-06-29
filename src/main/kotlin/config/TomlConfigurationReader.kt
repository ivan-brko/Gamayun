package config

import com.moandjiezana.toml.Toml
import java.io.File

class TomlConfigurationReader : ConfigurationReader {
    private fun File.parseTomlJobFile(): JobConfig =
        Toml().read(readText()).let { parsedToml ->
            val name = parsedToml.getString("name")
            val exePath = parsedToml.getString("pathToExecutable")
            val cronString = parsedToml.getString("cronString")
            val arguments = parsedToml.getList<String>("arguments")
            return JobConfig(name, exePath, arguments, cronString)
        }

    override fun readConfiguration(configurationRoot: String): List<JobConfig> =
        File(configurationRoot).walk()
            .filter { it.name.endsWith(".gamayunConf.toml") }
            .map { it.parseTomlJobFile() }
            .toList()
}