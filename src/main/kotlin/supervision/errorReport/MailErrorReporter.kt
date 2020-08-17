package supervision.errorReport

import config.ConfigurationReader
import mu.KotlinLogging
import org.kodein.di.DI
import org.kodein.di.instance
import org.simplejavamail.email.EmailBuilder
import org.simplejavamail.mailer.MailerBuilder

private val logger = KotlinLogging.logger {}

class MailErrorReporter(private val kodein: DI) : ErrorReporter {

    private val configurationReader: ConfigurationReader by kodein.instance()

    override fun reportErrorForJob(jobId: String, errorMessage: String?) {
        val errorReportConfig = configurationReader.readErrorReportingConfiguration()
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
            logger.warn("Invalid mail configuration!")
        }
    }
}