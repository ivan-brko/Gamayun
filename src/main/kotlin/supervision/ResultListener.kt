package supervision

interface ResultListener {
    fun listenForResult(jobId : String, timeoutMillis: Long): String?
}