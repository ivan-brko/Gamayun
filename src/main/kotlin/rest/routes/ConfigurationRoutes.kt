package rest.routes

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import observable.ObservableEvent
import observable.ObservableEventNotifier
import org.kodein.di.DI
import org.kodein.di.instance
import rest.BasicResponse

fun Routing.buildConfigurationRoutes(kodein: DI) {
    route("configuration") {
        buildReloadConfigurationRoute(kodein)
    }
}

fun Route.buildReloadConfigurationRoute(kodein: DI) {
    route("reloadConfiguration") {
        get {
            val observableEventNotifier: ObservableEventNotifier by kodein.instance()
            observableEventNotifier.triggerEvent(ObservableEvent.CONFIGURATION_RESTART)
            call.respond(BasicResponse(true))
        }
    }
}