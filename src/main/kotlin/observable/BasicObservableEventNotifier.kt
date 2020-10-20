package observable

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class BasicObservableEventNotifier : ObservableEventNotifier {
    private val registeredLambdas: MutableMap<ObservableEvent, MutableList<suspend () -> Unit>> =
        mutableMapOf()

    override fun subscribeToEvent(observableEvent: ObservableEvent, lambda: suspend () -> Unit) {
        if (registeredLambdas.containsKey(observableEvent)) {
            registeredLambdas[observableEvent]!!.add(lambda)
        } else {
            registeredLambdas[observableEvent] = mutableListOf(lambda)
        }
    }

    override fun triggerEvent(observableEvent: ObservableEvent) {
        logger.info { "Triggering event $observableEvent" }
        GlobalScope.async {
            registeredLambdas[observableEvent]?.forEach { lambda ->
                async { lambda() }
            }
        }
    }
}