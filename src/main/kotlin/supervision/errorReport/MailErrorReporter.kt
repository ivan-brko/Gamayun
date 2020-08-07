package supervision.errorReport

import config.ErrorReportConfig
import org.simplejavamail.email.EmailBuilder
import org.simplejavamail.mailer.MailerBuilder

class MailErrorReporter(private val errorReportConfig: ErrorReportConfig) : ErrorReporter {
    override fun reportErrorForJob(jobId: String, errorMessage: String?) {
        val mailConfiguration = errorReportConfig.mailConfiguration

        if (mailConfiguration != null) {
            val email = EmailBuilder.startingBlank()
                .to(mailConfiguration.recipientName, mailConfiguration.recipientEmail)
                .from(mailConfiguration.senderName, mailConfiguration.senderEmail)
                .withSubject("Gamayun report for job $jobId")
                .withPlainText(errorMessage ?: "Did not receive results for $jobId")
                .buildEmail()

            MailerBuilder.withSMTPServer(
                mailConfiguration.smtpHost,
                mailConfiguration.smtpPort.toInt(),
                mailConfiguration.mailUsername,
                mailConfiguration.mailPassword
            ).buildMailer()
                .sendMail(email)
        } else {
            //todo: Log!
        }
    }
}