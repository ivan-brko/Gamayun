package scheduling

import config.JobConfig
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.kodein.di.DI
import org.kodein.di.instance
import org.quartz.*
import org.quartz.impl.StdSchedulerFactory
import supervision.TaskSupervisor
import supervision.toTaskConfig
import java.util.*
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

class GamayunJob : Job {
    override fun execute(context: JobExecutionContext?) {
        val jobDataMap = context?.jobDetail?.jobDataMap

        if (jobDataMap != null) {
            GlobalScope.launch {
                val jobConfig = jobDataMap["jobConfig"] as JobConfig
                val supervisor = jobDataMap["taskSupervisor"] as TaskSupervisor
                logger.info { "Triggering job ${jobConfig.name}" }

                if (jobConfig.randomTriggerOffsetSeconds != null) {
                    val scheduler = jobDataMap["scheduler"] as org.quartz.Scheduler
                    val trigger = QuartzScheduler.buildTriggerForRandomOffset(
                        jobConfig.name,
                        jobConfig.cron,
                        jobConfig.randomTriggerOffsetSeconds
                    )
                    QuartzScheduler.createAndScheduleJob(jobConfig, jobDataMap, scheduler, trigger)
                }
                supervisor.runTask(jobConfig.toTaskConfig())
            }
        } else {
            logger.warn("Error! Did not receive JobExecutionContext while triggering a job")
        }
    }
}

class QuartzScheduler(kodein: DI) : Scheduler {

    private val taskSupervisor: TaskSupervisor by kodein.instance()

    override fun scheduleJobs(jobs: List<JobConfig>) {
        val scheduler = StdSchedulerFactory().scheduler

        jobs.forEach { jobConfig -> scheduleJob(jobConfig, scheduler) }

        scheduler.start()
    }

    private fun scheduleJob(jobConfig: JobConfig, scheduler: org.quartz.Scheduler) {
        logger.info { "Scheduling job ${jobConfig.name}" }
        val jobDataMap = createJobDataMap(jobConfig, scheduler)
        val trigger = createTrigger(jobConfig)
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

    private fun createTrigger(jobConfig: JobConfig): Trigger =
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