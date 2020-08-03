package supervision

import config.JobConfig

data class TaskConfig(
    val name: String,
    val pathToExe: String,
    val args: List<String>,
    val resultWaitTimeoutMillis: Long
)

fun JobConfig.toTaskConfig() =
    TaskConfig(name, pathToExecutable, args, resultWaitTimeoutMillis)

interface TaskSupervisor {
    suspend fun runTask(taskConfig: TaskConfig)
}