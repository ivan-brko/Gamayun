package notification

import org.kodein.di.DI

object NotificationSetup {
    fun setupNotificationSupport(kodein: DI): List<Notifier> {
        val mailNotifier = MailNotifier.createMailErrorReporter(kodein)

        return with(mutableListOf<Notifier>()) {
            if (mailNotifier != null) {
                add(mailNotifier)
            }

            //todo: add all future notifiers here

            this
        }
    }
}