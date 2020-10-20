package scheduling.quartz

import config.JobConfig
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import notification.Notifier
import org.quartz.Job
import org.quartz.JobExecutionContext
import supervision.TaskSupervisor
import supervision.toTaskConfig

private val logger = KotlinLogging.logger {}

class HeartbeatJob : Job {
    override fun execute(context: JobExecutionContext?) {
        val jobDataMap = context?.jobDetail?.jobDataMap

        if (jobDataMap != null) {
            GlobalScope.launch {
                val notifiers = jobDataMap["notifiers"] as List<Notifier>
                notifiers.forEach { it.sendHeartbeat() }
            }
        } else {
            logger.warn("Error! Did not receive JobExecutionContext while triggering heartbeat")
        }
    }

}

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