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

    val gamayunResultService = GamayunResultServer()
    gamayunResultService.start()

    val listener = PrimitiveGrpcResultListener(gamayunResultService)
    val repository = PrimitiveDataRepository()
    val supervisor = BasicTaskSupervisor(listener, repository)
    val scheduler = QuartzScheduler(supervisor)
    scheduler.scheduleJobs(configuration)

//    runBlocking{
//        val output = supervisor.runCommand(configuration.first().pathToExecutable, configuration.first().args)
//        println(output)
//    }

    readLine()
}