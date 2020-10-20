package supervision

import arrow.core.Either
import supervision.grpc.GamayunResult

interface ResultListener {
    suspend fun listenForResult(jobId: String, timeoutMillis: Long): Either<String, GamayunResult>?
}