package supervision

import config.JobConfig

data class TaskConfig(
    val name: String,
    val pathToExe: String,
    val args: List<String>
)

fun JobConfig.toTaskConfig() =
    TaskConfig(name, pathToExecutable, args)

interface TaskSupervisor {
    suspend fun runTask(taskConfig: TaskConfig)
}