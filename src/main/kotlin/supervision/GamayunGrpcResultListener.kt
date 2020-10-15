package supervision

import arrow.core.Either

class GamayunGrpcResultListener(private val grpcResultServer: GrpcResultServer) : ResultListener {
    override suspend fun listenForResult(jobId: String, timeoutMillis: Long): Either<String, List<String>>? =
            grpcResultServer.getResultsForJobId(jobId, timeoutMillis)
}