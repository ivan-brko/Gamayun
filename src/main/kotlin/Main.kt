import config.TomlConfigurationReader
import kotlinx.coroutines.runBlocking
import storage.PrimitiveDataRepository
import supervision.BasicSupervisor
import supervision.GamayunResultServer
import supervision.GamayunResultService
import supervision.PrimitiveGrpcResultListener

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

    val listener = PrimitiveGrpcResultListener()
    val repository = PrimitiveDataRepository()
    val supervisor = BasicSupervisor(listener, repository)
    val gamayunResultService = GamayunResultServer()
    gamayunResultService.start()
    runBlocking{
        val output = supervisor.runCommand(configuration.first().pathToExecutable, configuration.first().args)
        println(output)
    }

    readLine()
}