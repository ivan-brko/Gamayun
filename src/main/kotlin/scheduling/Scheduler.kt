package scheduling

interface Scheduler{
    fun scheduleTaskForExecution(task: (Unit) -> Unit, cron: String)
}