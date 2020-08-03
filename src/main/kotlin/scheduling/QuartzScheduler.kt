package scheduling

import config.JobConfig
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.quartz.*
import org.quartz.impl.StdSchedulerFactory
import supervision.TaskSupervisor
import supervision.toTaskConfig

private val logger = KotlinLogging.logger {}

class GamayunJob : Job {
    override fun execute(context: JobExecutionContext?) {
        val jobDataMap = context?.jobDetail?.jobDataMap

        if (jobDataMap != null) {
            GlobalScope.launch {
                val jobConfig = jobDataMap["jobConfig"] as JobConfig
                val supervisor = jobDataMap["taskSupervisor"] as TaskSupervisor
                logger.info { "Triggering job ${jobConfig.name}" }
                logger.debug {
                    "Job: ${jobConfig.name}, jobExecutable: ${jobConfig.pathToExecutable}, " +
                            "jobCron: ${jobConfig.cron}, jobArgs: ${jobConfig.args} "
                }
                supervisor.runTask(jobConfig.toTaskConfig())
            }
        } else {
            logger.warn("Error! Did not receive JobExecutionContext while triggering a job")
        }
    }
}

class QuartzScheduler(private val taskSupervisor: TaskSupervisor) : Scheduler {

    override fun scheduleJobs(jobs: List<JobConfig>) {
        val scheduler = StdSchedulerFactory().scheduler

        jobs.forEach { jobConfig -> scheduleJob(jobConfig, scheduler) }

        scheduler.start()
    }

    private fun scheduleJob(jobConfig: JobConfig, scheduler: org.quartz.Scheduler) {
        logger.info { "Scheduling job ${jobConfig.name}" }
        val jobDataMap = JobDataMap()
        jobDataMap["jobConfig"] = jobConfig
        jobDataMap["taskSupervisor"] = taskSupervisor

        val job =
            JobBuilder.newJob(GamayunJob::class.java)
                .withIdentity(jobConfig.name)
                .usingJobData(jobDataMap)
                .build()

        val trigger =
            TriggerBuilder.newTrigger()
                .withIdentity(jobConfig.name)
                .startNow()
                .withSchedule(CronScheduleBuilder.cronSchedule(jobConfig.cron))
                .build()

        scheduler.scheduleJob(job, trigger)
    }
}