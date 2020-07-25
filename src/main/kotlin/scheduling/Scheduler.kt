package scheduling

import config.JobConfig

interface Scheduler {
    fun scheduleJobs(jobs: List<JobConfig>): Unit
}