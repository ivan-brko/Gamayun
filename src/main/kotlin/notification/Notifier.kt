package notification

interface Notifier {
    fun reportErrorForJob(jobId: String, errorMessage: String? = null)
    fun reportGenericError(errorName: String, errorMessage: String? = null)
    fun sendHeartbeat()
}