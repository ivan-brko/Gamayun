import config.TomlConfigurationReader
import mu.KotlinLogging
import scheduling.QuartzScheduler
import storage.mongo.MongoDataRepository
import storage.mongo.MongoDbSettings
import supervision.BasicTaskSupervisor
import supervision.GamayunGrpcResultListener
import supervision.GrpcResultServer
import supervision.errorReport.MailErrorReporter


fun main(args: Array<String>) {
    val logger = KotlinLogging.logger {}
    logger.info { "Starting Gamayun application" }
    val configurationReader = TomlConfigurationReader("/home/brko/temp/gconf")
    val jobsConfiguration = configurationReader.readJobsConfiguration()

    val resultServer = GrpcResultServer()
    resultServer.start()

    val listener = GamayunGrpcResultListener(resultServer)
    val mongoDbSettings = MongoDbSettings(configurationReader.readDatabaseConfiguration())
    val repository = MongoDataRepository(mongoDbSettings)
    val mailErrorReporter = MailErrorReporter(configurationReader.readErrorReportingConfiguration())
    val supervisor = BasicTaskSupervisor(listener, repository, mailErrorReporter)
    val scheduler = QuartzScheduler(supervisor)
    scheduler.scheduleJobs(jobsConfiguration)

    readLine()
}