package supervision.errorReport

import config.ConfigurationReader
import config.MailConfiguration
import mu.KotlinLogging
import org.kodein.di.DI
import org.kodein.di.instance
import org.simplejavamail.email.EmailBuilder
import org.simplejavamail.mailer.MailerBuilder

private val logger = KotlinLogging.logger {}

class MailErrorReporter private constructor(private val mailConfiguration: MailConfiguration) : ErrorReporter {
    override fun reportErrorForJob(jobId: String, errorMessage: String?) {
        val email = EmailBuilder.startingBlank()
            .to(mailConfiguration.recipientName, mailConfiguration.recipientEmail)
            .from(mailConfiguration.senderName, mailConfiguration.senderEmail)
            .withSubject("Gamayun report for job $jobId")
            .withPlainText(errorMessage ?: "Did not receive results for $jobId")
            .buildEmail()

        logger.warn {
            "Sending email error report for job $jobId with error message $errorMessage " +
                    "to ${mailConfiguration.recipientEmail} from ${mailConfiguration.senderEmail}"
        }

        MailerBuilder.withSMTPServer(
            mailConfiguration.smtpHost,
            mailConfiguration.smtpPort.toInt(),
            mailConfiguration.mailUsername,
            mailConfiguration.mailPassword
        ).buildMailer()
            .sendMail(email)
    }

    companion object {
        fun createMailErrorReporter(kodein: DI): MailErrorReporter? {
            val configurationReader: ConfigurationReader by kodein.instance()
            val errorReportConfig = configurationReader.readErrorReportingConfiguration()
            val mailConfiguration = errorReportConfig.mailConfiguration
            return if (mailConfiguration != null) {
                MailErrorReporter(mailConfiguration)
            } else {
                null
            }
        }
    }
}