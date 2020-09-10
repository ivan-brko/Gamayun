package errorReport

interface ErrorReporter {
    fun reportErrorForJob(jobId: String, errorMessage: String? = null)
    fun reportGenericError(errorName: String, errorMessage: String? = null)
}