import config.TomlConfigurationReader
import kotlinx.coroutines.runBlocking
import scheduling.QuartzScheduler
import storage.PrimitiveDataRepository
import supervision.BasicTaskSupervisor
import supervision.GamayunResultServer
import supervision.PrimitiveGrpcResultListener

fun main(args: Array<String>) {
    val configuration = TomlConfigurationReader().readConfiguration("/home/brko/temp/gconf")


//    scheduler.testScheduling()

    val listener = PrimitiveGrpcResultListener()
    val repository = PrimitiveDataRepository()
    val supervisor = BasicTaskSupervisor(listener, repository)
    val scheduler = QuartzScheduler(supervisor)
    scheduler.scheduleJobs(configuration)
    val gamayunResultService = GamayunResultServer()
    gamayunResultService.start()
//    runBlocking{
//        val output = supervisor.runCommand(configuration.first().pathToExecutable, configuration.first().args)
//        println(output)
//    }

    readLine()
}