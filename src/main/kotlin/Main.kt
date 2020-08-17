import config.ConfigurationReader
import di.KodeinSetup.setupDi
import mu.KotlinLogging
import org.kodein.di.instance
import scheduling.Scheduler


fun main(args: Array<String>) {
    val logger = KotlinLogging.logger {}
    logger.info { "Starting Gamayun application" }
    /*val configurationReader = TomlConfigurationReader("/home/brko/temp/gconf")
    val jobsConfiguration = configurationReader.readJobsConfiguration()

    val resultServer = GrpcResultServer()
    resultServer.start()

    val listener = GamayunGrpcResultListener(resultServer)
    val mongoDbSettings = MongoDbSettings(configurationReader.readDatabaseConfiguration())
    val repository = MongoDataRepository(mongoDbSettings)
    val mailErrorReporter = MailErrorReporter(configurationReader.readErrorReportingConfiguration())
    val supervisor = BasicTaskSupervisor(listener, repository, mailErrorReporter)
    val scheduler = QuartzScheduler(supervisor)*/

    val configurationRoot = System.getenv("GAMAYUN_CONF_ROOT")
        ?: throw IllegalArgumentException("Configuration Root (GAMAYUN_CONF_ROOT) env var not set")

    val kodein = setupDi(configurationRoot)
    val scheduler : Scheduler by kodein.instance()
    val configurationReader: ConfigurationReader by kodein.instance()

    scheduler.scheduleJobs(configurationReader.readJobsConfiguration())

    readLine()
}