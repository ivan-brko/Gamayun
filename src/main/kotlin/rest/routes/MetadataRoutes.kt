package rest.routes

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import org.kodein.di.DI
import rest.Version

fun Routing.buildMetadataRoutes(kodein: DI) {
    route("metadata") {
        buildVersionRoute()
    }
}

fun Route.buildVersionRoute() {
    route("version") {
        get {
            call.respond(Version(0, 2, 1))
        }
    }
}