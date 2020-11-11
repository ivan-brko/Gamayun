package supervision

import config.JobConfig
import config.JobDuplicateEntryPolicy

data class TaskConfig(
    val name: String,
    val pathToExe: String,
    val args: List<String>?,
    val resultWaitTimeoutMillis: Long,
    val tags: List<String>?,
    val jobDuplicateEntryPolicy: JobDuplicateEntryPolicy?,
    val producesResult: Boolean
)

fun JobConfig.toTaskConfig() =
    TaskConfig(name, pathToExecutable, args, resultWaitTimeoutMillis, tags, jobDuplicateEntryPolicy, producesResult)

interface TaskSupervisor {
    suspend fun runTask(taskConfig: TaskConfig)
}