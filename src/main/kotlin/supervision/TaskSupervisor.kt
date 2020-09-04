package supervision

import config.JobConfig

data class TaskConfig(
    val name: String,
    val pathToExe: String,
    val args: List<String>?,
    val resultWaitTimeoutMillis: Long,
    val tags: List<String>?,
    val uniqueIds: List<String>?
)

fun JobConfig.toTaskConfig() =
    TaskConfig(name, pathToExecutable, args, resultWaitTimeoutMillis, tags, uniqueIds)

interface TaskSupervisor {
    suspend fun runTask(taskConfig: TaskConfig)
}