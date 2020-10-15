import di.KodeinSetup.setupDi
import mu.KotlinLogging
import org.kodein.di.instance
import scheduling.GamayunInitializer


fun main(args: Array<String>) {
    val logger = KotlinLogging.logger {}
    logger.info { "Starting Gamayun application" }
    val configurationRoot = System.getenv("GAMAYUN_CONF_ROOT")
            ?: throw IllegalArgumentException("Configuration Root (GAMAYUN_CONF_ROOT) env var not set")

    val kodein = setupDi(configurationRoot)
    val gamayunInitializer: GamayunInitializer by kodein.instance()
    gamayunInitializer.scheduleJobsAndHeartbeat()

    //todo: look if this can be implemented nicer in kotlin
    readLine()
}