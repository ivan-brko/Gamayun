import config.MongoDbConfig
import config.TomlConfigurationReader
import mu.KotlinLogging
import scheduling.QuartzScheduler
import storage.PrimitiveDataRepository
import storage.mongo.MongoDataRepository
import storage.mongo.MongoDbSettings
import supervision.BasicTaskSupervisor
import supervision.GamayunGrpcResultListener
import supervision.GrpcResultServer


fun main(args: Array<String>) {
    val logger = KotlinLogging.logger {}
    logger.info { "Starting Gamayun application" }
    val configuration =
        TomlConfigurationReader()
            .readJobsConfiguration("/home/brko/temp/gconf")

    val resultServer = GrpcResultServer()
    resultServer.start()

    val listener = GamayunGrpcResultListener(resultServer)
    val mongoDbSettings = MongoDbSettings(TomlConfigurationReader().readDatabaseConfiguration("/home/brko/temp/gconf"))
    val repository = MongoDataRepository(mongoDbSettings)
    val supervisor = BasicTaskSupervisor(listener, repository)
    val scheduler = QuartzScheduler(supervisor)
    scheduler.scheduleJobs(configuration)

    readLine()
}