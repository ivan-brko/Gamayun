package supervision

import config.JobConfig
import config.JobDuplicateEntryPolicy

data class TaskConfig(
        val name: String,
        val pathToExe: String,
        val args: List<String>?,
        val resultWaitTimeoutMillis: Long,
        val tags: List<String>?,
        val duplicateEntryPolicy: JobDuplicateEntryPolicy?
)

fun JobConfig.toTaskConfig() =
        TaskConfig(name, pathToExecutable, args, resultWaitTimeoutMillis, tags, jobDuplicateEntryPolicy)

interface TaskSupervisor {
    suspend fun runTask(taskConfig: TaskConfig)
}