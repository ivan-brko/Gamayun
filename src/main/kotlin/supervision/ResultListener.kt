package supervision

import arrow.core.Either

interface ResultListener {
    suspend fun listenForResult(jobId: String, timeoutMillis: Long): Either<String, List<String>>?
}