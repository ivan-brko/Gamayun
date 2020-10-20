package rest.routes

import io.ktor.application.*
import io.ktor.routing.*
import org.kodein.di.DI

fun Application.buildRoutes(kodein: DI) {
    routing {
        buildConfigurationRoutes(kodein)
        buildMetadataRoutes(kodein)
    }
}