package scheduling

import config.JobConfig
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.quartz.*
import org.quartz.impl.StdSchedulerFactory
import supervision.TaskSupervisor
import supervision.toTaskConfig

class GamayunJob : Job {
    override fun execute(context: JobExecutionContext?) {
        val jobDataMap = context?.jobDetail?.jobDataMap

        if (jobDataMap != null) {
            GlobalScope.launch {
                val jobConfig = jobDataMap["jobConfig"] as JobConfig
                println("Executing job ${jobConfig.name}")
                val supervisor = jobDataMap["taskSupervisor"] as TaskSupervisor
                supervisor.runTask(jobConfig.toTaskConfig())
            }
        } else {
            println("Error! Did not receive JobExecutionContext")
        }
    }

}

class QuartzScheduler(private val taskSupervisor: TaskSupervisor) : Scheduler {
    override fun scheduleJobs(jobs: List<JobConfig>) {
        val scheduler = StdSchedulerFactory().scheduler

        jobs.forEach {
            val jobDataMap = JobDataMap()
            jobDataMap["jobConfig"] = it
            jobDataMap["taskSupervisor"] = taskSupervisor

            val job = JobBuilder.newJob(GamayunJob::class.java).withIdentity(it.name).usingJobData(jobDataMap).build()
            val trigger = TriggerBuilder.newTrigger().withIdentity(it.name).startNow()
                .withSchedule(CronScheduleBuilder.cronSchedule(it.cron)).build()

            scheduler.scheduleJob(job, trigger)
        }

        scheduler.start()
    }
}