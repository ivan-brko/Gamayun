package scheduling

import config.ConfigurationReader
import config.JobConfig
import mu.KotlinLogging
import notification.Notifier
import org.kodein.di.DI
import org.kodein.di.instance
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

class GamayunInitializer(kodein: DI) {
    private val scheduler: Scheduler by kodein.instance()
    val configurationReader: ConfigurationReader by kodein.instance()
    private val notifiers: List<Notifier> by kodein.instance()

    fun scheduleJobsAndHeartbeat() {
        val jobs = configurationReader.readJobsConfiguration()
        val applicationConfiguration = configurationReader.readApplicationConfiguration()
        validateJobNamesOrKillApplication(jobs)
        scheduler.scheduleJobs(jobs)
        if (applicationConfiguration.heartbeatPeriodInSeconds != null) {
            scheduler.scheduleHeartbeat(applicationConfiguration.heartbeatPeriodInSeconds)
        }
        scheduler.startRunningTasks()
    }

    private fun validateJobNamesOrKillApplication(jobs: List<JobConfig>) {
        val groupedJobs = jobs.groupBy { it.name }
        if (groupedJobs.any { it.value.size > 1 }) {
            val duplicateJobNames = groupedJobs.filter { it.value.size > 1 }.map { it.key }
            val errorMessage = "The following names are duplicates: $duplicateJobNames"
            logger.error { "Invalid job configuration, duplicate names present, will kill application!\n$errorMessage" }
            notifiers.forEach {
                it.reportGenericError("Invalid job configuration, duplicate job names", errorMessage)
            }
            exitProcess(1)
        }
    }
}