import config.ConfigurationReader
import config.TomlConfigurationReader

fun main(args: Array<String>) {
    val configuration = TomlConfigurationReader().readConfiguration("/home/brko/temp/gconf")
    configuration.forEach {
        println(it.name)
        println("cron --- ${it.cron}")
        println("exe --- ${it.pathToExecutable}")
        it.args.forEachIndexed() { index, arg ->
            println("arg $index --- $arg")
        }
    }
}