package config

import com.moandjiezana.toml.Toml
import java.io.File

class TomlConfigurationReader : ConfigurationReader {
    private fun File.parseTomlJobFile(): JobConfig =
        Toml().read(readText()).run {
            val name = getString("name")
            val exePath = getString("pathToExecutable")
            val cronString = getString("cronString")
            val arguments = getList<String>("arguments")
            return JobConfig(name, exePath, arguments, cronString)
        }

    override fun readConfiguration(configurationRoot: String): List<JobConfig> =
        File(configurationRoot).walk()
            .filter { it.name.endsWith(".gamayunConf.toml") }
            .map { it.parseTomlJobFile() }
            .toList()
}