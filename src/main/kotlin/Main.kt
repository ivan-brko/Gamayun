import config.TomlConfigurationReader
import mu.KotlinLogging
import scheduling.QuartzScheduler
import storage.PrimitiveDataRepository
import supervision.BasicTaskSupervisor
import supervision.GamayunResultServer
import supervision.PrimitiveGrpcResultListener


fun main(args: Array<String>) {
    val logger = KotlinLogging.logger{}
    logger.info { "Starting Gamayun application" }
    val configuration = TomlConfigurationReader().readConfiguration("/home/brko/temp/gconf")

    val gamayunResultService = GamayunResultServer()
    gamayunResultService.start()

    val listener = PrimitiveGrpcResultListener(gamayunResultService)
    val repository = PrimitiveDataRepository()
    val supervisor = BasicTaskSupervisor(listener, repository)
    val scheduler = QuartzScheduler(supervisor)
    scheduler.scheduleJobs(configuration)

    readLine()
}