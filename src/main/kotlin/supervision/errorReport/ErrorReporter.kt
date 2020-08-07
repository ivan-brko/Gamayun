package supervision.errorReport

interface ErrorReporter {
    fun reportErrorForJob(jobId: String, errorMessage: String? = null)
}