package rest

import di.KodeinSetup
import io.ktor.application.*
import org.kodein.di.DI
import org.kodein.di.instance
import rest.routes.buildRoutes
import scheduling.GamayunInitializer

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    val configurationRoot = System.getenv("GAMAYUN_CONF_ROOT")
        ?: throw IllegalArgumentException("Configuration Root (GAMAYUN_CONF_ROOT) env var not set")

    val kodein = KodeinSetup.setupDi(configurationRoot)
    moduleWithInjectedDependencies(kodein, testing)
}

//as suggested in ktor documentation, we separate the function in two modules, where
//one simply creates DI object and passes it to the other
//this way, we can test easily by calling second rest.module with DI setup for tests
fun Application.moduleWithInjectedDependencies(kodein: DI, testing: Boolean) {
    val gamayunInitializer: GamayunInitializer by kodein.instance()
    gamayunInitializer.scheduleJobsAndHeartbeat()
    buildRoutes(kodein)
    setupContentNegotiation()
}