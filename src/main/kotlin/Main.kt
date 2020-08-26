import config.ConfigurationReader
import di.KodeinSetup.setupDi
import mu.KotlinLogging
import org.kodein.di.instance
import scheduling.Scheduler


fun main(args: Array<String>) {
    val logger = KotlinLogging.logger {}
    logger.info { "Starting Gamayun application" }
    val configurationRoot = System.getenv("GAMAYUN_CONF_ROOT")
        ?: throw IllegalArgumentException("Configuration Root (GAMAYUN_CONF_ROOT) env var not set")

    val kodein = setupDi(configurationRoot)
    val scheduler: Scheduler by kodein.instance()
    val configurationReader: ConfigurationReader by kodein.instance()

    scheduler.scheduleJobs(configurationReader.readJobsConfiguration())

    //todo: look if this can be implemented nicer in kotlin
    readLine()
}