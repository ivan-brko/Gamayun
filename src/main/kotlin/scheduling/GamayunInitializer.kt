package scheduling

import config.ConfigurationReader
import config.JobConfig
import errorReport.ErrorReporter
import mu.KotlinLogging
import org.kodein.di.DI
import org.kodein.di.instance
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

class GamayunInitializer(kodein: DI) {
    val scheduler: Scheduler by kodein.instance()
    val configurationReader: ConfigurationReader by kodein.instance()
    private val errorReporters: List<ErrorReporter> by kodein.instance()

    fun validateJobNamesAndCallScheduler() {
        val jobs = configurationReader.readJobsConfiguration()
        validateJobNamesOrKillApplication(jobs)
        scheduler.scheduleJobs(jobs)
    }

    private fun validateJobNamesOrKillApplication(jobs: List<JobConfig>) {
        val groupedJobs = jobs.groupBy { it.name }
        if (groupedJobs.any { it.value.size > 1 }) {
            val duplicateJobNames = groupedJobs.filter { it.value.size > 1 }.map { it.key }
            val errorMessage = "The following names are duplicates: $duplicateJobNames"
            logger.error { "Invalid job configuration, duplicate names present, will kill application!\n$errorMessage" }
            errorReporters.forEach {
                it.reportGenericError("Invalid job configuration, duplicate job names", errorMessage)
            }
            exitProcess(1)
        }
    }
}