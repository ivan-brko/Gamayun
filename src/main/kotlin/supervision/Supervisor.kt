package supervision

data class TaskInfo(
    val name: String,
    val pathToExe: String,
    val args: List<String>
)

interface Supervisor {
    suspend fun runTask(taskInfo: TaskInfo)
}