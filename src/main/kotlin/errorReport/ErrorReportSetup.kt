package errorReport

import org.kodein.di.DI

object ErrorReportSetup {
    fun setupErrorSupport(kodein: DI): List<ErrorReporter> {
        val mailErrorReporter = MailErrorReporter.createMailErrorReporter(kodein)

        return with(mutableListOf<ErrorReporter>()) {
            if (mailErrorReporter != null) {
                add(mailErrorReporter)
            }

            //todo: add all future error reporters here

            this
        }
    }
}