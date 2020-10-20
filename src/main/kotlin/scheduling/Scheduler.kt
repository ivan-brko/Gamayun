package scheduling

import config.JobConfig

interface Scheduler {
    fun scheduleJobs(jobs: List<JobConfig>)
    fun scheduleHeartbeat(periodInSeconds: Long)
    fun startRunningTasks()
    fun deleteAllScheduledTasks()
}