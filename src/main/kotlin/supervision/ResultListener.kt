package supervision

interface ResultListener {
    suspend fun listenForResult(jobId : String, timeoutMillis: Long): List<String>?
}