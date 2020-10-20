package scheduling.quartz

import config.JobConfig
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import notification.Notifier
import org.kodein.di.DI
import org.kodein.di.instance
import org.quartz.*
import org.quartz.SimpleScheduleBuilder.simpleSchedule
import org.quartz.impl.StdSchedulerFactory
import scheduling.Scheduler
import supervision.TaskSupervisor
import supervision.toTaskConfig
import java.util.*
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

class QuartzScheduler(kodein: DI) : Scheduler {

    private val taskSupervisor: TaskSupervisor by kodein.instance()
    private val notifiers: List<Notifier> by kodein.instance()
    private val scheduler = StdSchedulerFactory().scheduler


    override fun scheduleJobs(jobs: List<JobConfig>) {
        jobs.forEach { jobConfig -> scheduleJob(jobConfig, scheduler) }
    }

    override fun scheduleHeartbeat(periodInSeconds: Long) {
        logger.info { "Scheduling heartbeat to run every $periodInSeconds seconds" }
        val dataMap = createHeartbeatDataMap(notifiers)
        val trigger = createHeartbeatTrigger(periodInSeconds)

        createAndScheduleHeartbeat(dataMap, scheduler, trigger)
    }

    override fun startRunningTasks() {
        logger.info { "Starting all the tasks" }
        scheduler.start()
    }

    override fun deleteAllScheduledTasks() {
        logger.info { "Deleting all previously scheduled tasks" }
        scheduler.clear()
    }

    private fun scheduleJob(jobConfig: JobConfig, scheduler: org.quartz.Scheduler) {
        logger.info { "Scheduling job ${jobConfig.name}" }
        val jobDataMap = createJobDataMap(jobConfig, scheduler)
        val trigger = createJobTrigger(jobConfig)
        createAndScheduleJob(jobConfig, jobDataMap, scheduler, trigger)
    }

    private fun createJobDataMap(jobConfig: JobConfig, scheduler: org.quartz.Scheduler): JobDataMap =
        JobDataMap().also {
            it["jobConfig"] = jobConfig
            it["taskSupervisor"] = taskSupervisor
            if (jobConfig.randomTriggerOffsetSeconds != null) {
                it["scheduler"] = scheduler
            }
        }

    private fun createHeartbeatDataMap(notifiers: List<Notifier>): JobDataMap =
        JobDataMap().also {
            it["notifiers"] = notifiers
        }

    private fun createHeartbeatTrigger(heartbeatPeriodInSeconds: Long): Trigger =
        TriggerBuilder.newTrigger()
            .startNow()
            .withSchedule(simpleSchedule().withIntervalInSeconds(heartbeatPeriodInSeconds.toInt()).repeatForever())
            .build()

    private fun createJobTrigger(jobConfig: JobConfig): Trigger =
        if (jobConfig.randomTriggerOffsetSeconds != null) {
            buildTriggerForRandomOffset(
                jobConfig.name,
                jobConfig.cron,
                jobConfig.randomTriggerOffsetSeconds
            )
        } else {
            TriggerBuilder.newTrigger()
                .withIdentity(jobConfig.name, "gamayun")
                .startNow()
                .withSchedule(CronScheduleBuilder.cronSchedule(jobConfig.cron))
                .build()
        }

    companion object {
        fun buildTriggerForRandomOffset(
            jobId: String,
            cron: String,
            maxOffsetSeconds: Long
        ): Trigger {
            val cronExpression = CronExpression(cron)
            val nextOccurrenceWithoutRandom = cronExpression.getNextValidTimeAfter(Date())
            val realOffset = Random.nextLong(maxOffsetSeconds)
            val nextOccurrenceWithRandom = Date()
            nextOccurrenceWithRandom.time = nextOccurrenceWithoutRandom.time + (realOffset * 1000)

            val nextOccurrence =
                if (nextOccurrenceWithRandom.after(Date())) { //if calculated next occurrence is after NOW
                    nextOccurrenceWithRandom
                } else {
                    logger.warn("Calculated next random occurrence for job $jobId is after NOW. Will use next non-random occurrence instead!")
                    cronExpression.getNextValidTimeAfter(Date())
                }

            return TriggerBuilder.newTrigger()
                .withIdentity(jobId, "gamayun")
                .startAt(nextOccurrence)
                .build()
        }

        fun createAndScheduleHeartbeat(
            dataMap: JobDataMap,
            scheduler: org.quartz.Scheduler,
            trigger: Trigger
        ) {
            val hb =
                JobBuilder.newJob(HeartbeatJob::class.java)
                    .withIdentity("heartbeat", "gamayun")
                    .usingJobData(dataMap)
                    .build()

            scheduler.scheduleJob(hb, trigger)
        }

        fun createAndScheduleJob(
            jobConfig: JobConfig,
            jobDataMap: JobDataMap?,
            scheduler: org.quartz.Scheduler,
            trigger: Trigger
        ) {
            val job =
                JobBuilder.newJob(GamayunJob::class.java)
                    .withIdentity(jobConfig.name, "gamayun")
                    .usingJobData(jobDataMap)
                    .build()
            scheduler.scheduleJob(job, trigger)
        }
    }
}