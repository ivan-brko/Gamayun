package observable

enum class ObservableEvent {
    CONFIGURATION_RESTART
}

//TODO: this interface is trivial as it expects that events carry no additional information
//as other events get added in the future this will be changed to something more similar to c# events
interface ObservableEventNotifier {
    fun subscribeToEvent(observableEvent: ObservableEvent, lambda: suspend () -> Unit)
    fun triggerEvent(observableEvent: ObservableEvent)
}